package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice;
import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice.InvoiceDirection;
import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice.ValidationStatus;
import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice.SuggestedAccounts;
import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice.AccountInfo;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzedDocument;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentField;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.util.polling.SyncPoller;
import com.azure.core.util.BinaryData;
import com.azure.core.exception.HttpResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.AzureKeyCredential;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;


@Service
public class DocumentScanningService {

    private static final Logger log = LoggerFactory.getLogger(DocumentScanningService.class);

    private final ImageCompressionService imageCompressionService;

    public DocumentScanningService(ImageCompressionService imageCompressionService) {
        this.imageCompressionService = imageCompressionService;
    }

    /**
     * Recognize invoice from uploaded file.
     * Uses user-selected invoice type, with auto-detection as validation.
     */
    public RecognizedInvoice recognizeInvoice(MultipartFile file, String invoiceType, String endpoint, String key,
                                               CompanyEntity company) throws IOException {
        Path compressedFile = null;

        try {
            // Compress image if needed
            compressedFile = imageCompressionService.compressIfNeeded(file);

            DocumentAnalysisClient client = new DocumentAnalysisClientBuilder()
                    .credential(new AzureKeyCredential(key))
                    .endpoint(endpoint)
                    .buildClient();

            String modelId = "prebuilt-invoice";
            BinaryData binaryData;

            if (compressedFile != null) {
                // Use compressed file
                binaryData = BinaryData.fromFile(compressedFile);
                log.info("Using compressed file for analysis: {}", compressedFile.getFileName());
            } else {
                // Use original file
                binaryData = BinaryData.fromStream(file.getInputStream(), file.getSize());
            }

            SyncPoller<OperationResult, AnalyzeResult> poller = client.beginAnalyzeDocument(modelId, binaryData);
            AnalyzeResult analyzeResult = poller.getFinalResult();

            RecognizedInvoice invoice = toRecognizedInvoice(analyzeResult);

            // Determine invoice direction - use user selection with auto-detection as validation
            determineInvoiceDirection(invoice, company, invoiceType);

            // Set suggested accounts based on automation settings
            setSuggestedAccounts(invoice, company);

            // Set validation status
            setValidationStatus(invoice);

            return invoice;

        } catch (HttpResponseException e) {
            log.error("Azure Document Intelligence error: {}", e.getMessage());
            String userMessage = translateAzureError(e);
            throw new IllegalStateException(userMessage, e);
        } finally {
            // Clean up compressed file
            if (compressedFile != null) {
                imageCompressionService.deleteIfExists(compressedFile);
            }
        }
    }

    /**
     * Determine if this is a purchase or sale.
     * Primary: use user-selected invoiceType
     * Validation: compare VAT numbers with our company to verify selection
     */
    private void determineInvoiceDirection(RecognizedInvoice invoice, CompanyEntity company, String invoiceType) {
        String ourVatNumber = normalizeVatNumber(company.getVatNumber());
        String ourEik = company.getEik();

        String vendorVat = normalizeVatNumber(invoice.getVendorVatNumber());
        String customerVat = normalizeVatNumber(invoice.getCustomerVatNumber());

        log.info("Determining invoice direction: userSelected={}, ourVAT={}, ourEIK={}, vendorVAT={}, customerVAT={}",
                invoiceType, ourVatNumber, ourEik, vendorVat, customerVat);

        // Auto-detect direction from VAT numbers
        InvoiceDirection autoDetected = null;

        // Check if we are the customer (this is a purchase invoice)
        if (customerVat != null && ourEik != null &&
            (customerVat.equals(ourVatNumber) || customerVat.contains(ourEik))) {
            autoDetected = InvoiceDirection.PURCHASE;
        }
        // Check if we are the vendor (this is a sales invoice)
        else if (vendorVat != null && ourEik != null &&
                 (vendorVat.equals(ourVatNumber) || vendorVat.contains(ourEik))) {
            autoDetected = InvoiceDirection.SALE;
        }

        // Use user-selected direction as primary
        InvoiceDirection userDirection = "sales".equalsIgnoreCase(invoiceType)
            ? InvoiceDirection.SALE
            : InvoiceDirection.PURCHASE;

        invoice.setDirection(userDirection);
        log.info("Invoice direction set to: {} (user selected)", userDirection);

        // If auto-detection disagrees with user selection, flag for review but keep user selection
        if (autoDetected != null && autoDetected != userDirection) {
            invoice.setRequiresManualReview(true);
            invoice.setManualReviewReason(String.format(
                "Избрахте '%s', но автоматичната проверка показва '%s'. " +
                "ДДС номерата на фактурата показват различна посока. Моля, проверете избора.",
                userDirection == InvoiceDirection.PURCHASE ? "Покупка" : "Продажба",
                autoDetected == InvoiceDirection.PURCHASE ? "Покупка" : "Продажба"));
            log.warn("User selection differs from auto-detection: user={}, auto={}", userDirection, autoDetected);
        }
    }

    /**
     * Normalize VAT number for comparison (remove country prefix, spaces, special chars).
     */
    private String normalizeVatNumber(String vatNumber) {
        if (vatNumber == null || vatNumber.isBlank()) {
            return null;
        }
        // Remove spaces, dashes, and common prefixes
        String normalized = vatNumber.toUpperCase()
                .replaceAll("[\\s\\-\\.]+", "")
                .replaceAll("^BG", "");  // Remove Bulgarian prefix for comparison
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Set suggested accounts based on invoice direction and company automation settings.
     */
    private void setSuggestedAccounts(RecognizedInvoice invoice, CompanyEntity company) {
        SuggestedAccounts suggested = new SuggestedAccounts();

        if (invoice.getDirection() == InvoiceDirection.PURCHASE) {
            // Purchase invoice - we're buying
            if (company.getDefaultSuppliersAccount() != null) {
                suggested.setCounterpartyAccount(toAccountInfo(company.getDefaultSuppliersAccount()));
            }
            if (company.getDefaultVatPurchaseAccount() != null) {
                suggested.setVatAccount(toAccountInfo(company.getDefaultVatPurchaseAccount()));
            }
            // For expense account, we don't have a default - leave for user selection
        } else if (invoice.getDirection() == InvoiceDirection.SALE) {
            // Sales invoice - we're selling
            if (company.getDefaultCustomersAccount() != null) {
                suggested.setCounterpartyAccount(toAccountInfo(company.getDefaultCustomersAccount()));
            }
            if (company.getDefaultVatSalesAccount() != null) {
                suggested.setVatAccount(toAccountInfo(company.getDefaultVatSalesAccount()));
            }
            if (company.getDefaultSalesRevenueAccount() != null) {
                suggested.setExpenseOrRevenueAccount(toAccountInfo(company.getDefaultSalesRevenueAccount()));
            }
        }

        invoice.setSuggestedAccounts(suggested);
    }

    private AccountInfo toAccountInfo(AccountEntity account) {
        if (account == null) return null;
        return new AccountInfo(account.getId(), account.getCode(), account.getName());
    }

    /**
     * Set validation status based on available VAT numbers.
     */
    private void setValidationStatus(RecognizedInvoice invoice) {
        String relevantVatNumber;

        // For purchases, we need to validate the vendor's VAT
        // For sales, we need to validate the customer's VAT
        if (invoice.getDirection() == InvoiceDirection.PURCHASE) {
            relevantVatNumber = invoice.getVendorVatNumber();
        } else if (invoice.getDirection() == InvoiceDirection.SALE) {
            relevantVatNumber = invoice.getCustomerVatNumber();
        } else {
            // Unknown direction - check both
            relevantVatNumber = invoice.getVendorVatNumber() != null ?
                    invoice.getVendorVatNumber() : invoice.getCustomerVatNumber();
        }

        if (relevantVatNumber == null || relevantVatNumber.isBlank()) {
            invoice.setValidationStatus(ValidationStatus.MANUAL_REVIEW);
            invoice.setRequiresManualReview(true);
            if (invoice.getManualReviewReason() == null) {
                invoice.setManualReviewReason("Не е намерен ДДС номер на контрагента. Моля, проверете ръчно.");
            }
        } else {
            // VAT number found - set to pending for VIES validation
            invoice.setValidationStatus(ValidationStatus.PENDING);
        }
    }

    private String translateAzureError(HttpResponseException e) {
        String message = e.getMessage();

        if (message.contains("InvalidContentLength") || message.contains("too large")) {
            return "Файлът е твърде голям за обработка. Моля, използвайте файл с по-малък размер или резолюция.";
        }
        if (message.contains("InvalidContent") || message.contains("corrupt")) {
            return "Файлът е повреден или в неподдържан формат. Поддържани формати: JPEG, PNG, BMP, TIFF, PDF.";
        }
        if (message.contains("Unauthorized") || message.contains("401")) {
            return "Грешка при автентикация с Azure. Моля, проверете конфигурацията на компанията.";
        }
        if (message.contains("quota") || message.contains("429")) {
            return "Достигнат е лимитът за заявки към Azure. Моля, опитайте по-късно.";
        }

        return "Грешка при сканиране на документа. Моля, опитайте отново или се свържете с поддръжка.";
    }

    private RecognizedInvoice toRecognizedInvoice(AnalyzeResult analyzeResult) {
        RecognizedInvoice invoice = new RecognizedInvoice();

        for (AnalyzedDocument analyzedDocument : analyzeResult.getDocuments()) {
            Map<String, DocumentField> fields = analyzedDocument.getFields();

            // Vendor (seller) information
            if (fields.containsKey("VendorName")) {
                invoice.setVendorName(getFieldContent(fields.get("VendorName")));
            }
            if (fields.containsKey("VendorTaxId")) {
                invoice.setVendorVatNumber(getFieldContent(fields.get("VendorTaxId")));
            }
            if (fields.containsKey("VendorAddress")) {
                invoice.setVendorAddress(getFieldContent(fields.get("VendorAddress")));
            }

            // Customer (buyer) information
            if (fields.containsKey("CustomerName")) {
                invoice.setCustomerName(getFieldContent(fields.get("CustomerName")));
            }
            if (fields.containsKey("CustomerTaxId")) {
                invoice.setCustomerVatNumber(getFieldContent(fields.get("CustomerTaxId")));
            }
            if (fields.containsKey("CustomerAddress")) {
                invoice.setCustomerAddress(getFieldContent(fields.get("CustomerAddress")));
            }

            // Invoice details
            if (fields.containsKey("InvoiceId")) {
                invoice.setInvoiceId(getFieldContent(fields.get("InvoiceId")));
            }
            if (fields.containsKey("InvoiceDate")) {
                DocumentField dateField = fields.get("InvoiceDate");
                if (dateField != null && dateField.getValueAsDate() != null) {
                    invoice.setInvoiceDate(dateField.getValueAsDate());
                }
            }
            if (fields.containsKey("DueDate")) {
                DocumentField dateField = fields.get("DueDate");
                if (dateField != null && dateField.getValueAsDate() != null) {
                    invoice.setDueDate(dateField.getValueAsDate());
                }
            }

            // Financial amounts
            if (fields.containsKey("SubTotal")) {
                invoice.setSubtotal(getCurrencyAmount(fields.get("SubTotal")));
            }
            if (fields.containsKey("TotalTax")) {
                invoice.setTotalTax(getCurrencyAmount(fields.get("TotalTax")));
            }
            if (fields.containsKey("InvoiceTotal")) {
                invoice.setInvoiceTotal(getCurrencyAmount(fields.get("InvoiceTotal")));
            }

            // Log extracted VAT numbers for debugging
            log.info("Extracted VAT numbers - Vendor: {}, Customer: {}",
                    invoice.getVendorVatNumber(), invoice.getCustomerVatNumber());
        }

        return invoice;
    }

    private String getFieldContent(DocumentField field) {
        if (field == null) return null;
        String content = field.getContent();
        return content != null && !content.isBlank() ? content.trim() : null;
    }

    private BigDecimal getCurrencyAmount(DocumentField field) {
        if (field == null || field.getValueAsCurrency() == null) return null;
        return BigDecimal.valueOf(field.getValueAsCurrency().getAmount());
    }
}
