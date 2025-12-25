package bg.spacbg.sp_ac_bg.web.graphql;

import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity.InvoiceDirection;
import bg.spacbg.sp_ac_bg.repository.AccountRepository;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import bg.spacbg.sp_ac_bg.service.ScannedInvoiceService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("isAuthenticated()")
public class ScannedInvoiceResolver {

    private final ScannedInvoiceService scannedInvoiceService;
    private final CompanyService companyService;
    private final AccountRepository accountRepository;

    public ScannedInvoiceResolver(ScannedInvoiceService scannedInvoiceService,
                                   CompanyService companyService,
                                   AccountRepository accountRepository) {
        this.scannedInvoiceService = scannedInvoiceService;
        this.companyService = companyService;
        this.accountRepository = accountRepository;
    }

    @QueryMapping
    public List<ScannedInvoiceEntity> scannedInvoices(@Argument Integer companyId) {
        return scannedInvoiceService.findByCompany(companyId);
    }

    @QueryMapping
    public List<ScannedInvoiceEntity> scannedInvoicesByDirection(@Argument Integer companyId,
                                                                   @Argument String direction) {
        InvoiceDirection dir = InvoiceDirection.valueOf(direction);
        return scannedInvoiceService.findByCompanyAndDirection(companyId, dir);
    }

    @QueryMapping
    public ScannedInvoiceEntity scannedInvoice(@Argument Integer id) {
        return scannedInvoiceService.findById(id).orElse(null);
    }

    @MutationMapping
    public ScannedInvoiceEntity saveScannedInvoice(@Argument Integer companyId,
                                                    @Argument Map<String, Object> recognized,
                                                    @Argument String fileName) {
        CompanyEntity company = companyService.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        // Convert the input map to RecognizedInvoice
        RecognizedInvoice invoice = mapToRecognizedInvoice(recognized);

        return scannedInvoiceService.saveFromRecognized(invoice, company, null, fileName);
    }

    @MutationMapping
    public ScannedInvoiceEntity updateScannedInvoiceAccounts(@Argument Integer id,
                                                              @Argument Map<String, Object> input) {
        Integer counterpartyAccountId = getIntOrNull(input, "counterpartyAccountId");
        Integer vatAccountId = getIntOrNull(input, "vatAccountId");
        Integer expenseRevenueAccountId = getIntOrNull(input, "expenseRevenueAccountId");

        return scannedInvoiceService.updateAccounts(id, counterpartyAccountId, vatAccountId, expenseRevenueAccountId);
    }

    @MutationMapping
    public ScannedInvoiceEntity validateScannedInvoiceVies(@Argument Integer id) {
        return scannedInvoiceService.validateVies(id);
    }

    @MutationMapping
    public boolean deleteScannedInvoice(@Argument Integer id) {
        scannedInvoiceService.delete(id);
        return true;
    }

    @MutationMapping
    public ScannedInvoiceEntity rejectScannedInvoice(@Argument Integer id, @Argument String reason) {
        return scannedInvoiceService.reject(id, reason);
    }

    /**
     * Process a scanned invoice and create a journal entry with VAT.
     * Creates accounting entry:
     * - For PURCHASE: Debit expense + VAT, Credit supplier
     * - For SALE: Debit customer, Credit revenue + VAT
     */
    @MutationMapping
    public ScannedInvoiceEntity processScannedInvoice(@Argument Integer id) {
        Integer userId = getCurrentUserId();
        return scannedInvoiceService.processToJournalEntry(id, userId);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from authentication token
        return 1; // Placeholder - should be extracted from JWT token
    }

    private RecognizedInvoice mapToRecognizedInvoice(Map<String, Object> input) {
        RecognizedInvoice invoice = new RecognizedInvoice();

        invoice.setVendorName((String) input.get("vendorName"));
        invoice.setVendorVatNumber((String) input.get("vendorVatNumber"));
        invoice.setVendorAddress((String) input.get("vendorAddress"));

        invoice.setCustomerName((String) input.get("customerName"));
        invoice.setCustomerVatNumber((String) input.get("customerVatNumber"));
        invoice.setCustomerAddress((String) input.get("customerAddress"));

        invoice.setInvoiceId((String) input.get("invoiceId"));
        invoice.setInvoiceDate(parseDate(input.get("invoiceDate")));
        invoice.setDueDate(parseDate(input.get("dueDate")));

        invoice.setSubtotal(parseBigDecimal(input.get("subtotal")));
        invoice.setTotalTax(parseBigDecimal(input.get("totalTax")));
        invoice.setInvoiceTotal(parseBigDecimal(input.get("invoiceTotal")));

        String directionStr = (String) input.get("direction");
        if (directionStr != null) {
            invoice.setDirection(RecognizedInvoice.InvoiceDirection.valueOf(directionStr));
        }

        String validationStatusStr = (String) input.get("validationStatus");
        if (validationStatusStr != null) {
            invoice.setValidationStatus(RecognizedInvoice.ValidationStatus.valueOf(validationStatusStr));
        }

        Boolean requiresManualReview = (Boolean) input.get("requiresManualReview");
        invoice.setRequiresManualReview(requiresManualReview != null && requiresManualReview);
        invoice.setManualReviewReason((String) input.get("manualReviewReason"));

        // Set suggested accounts from input
        Integer counterpartyAccountId = getIntOrNull(input, "counterpartyAccountId");
        Integer vatAccountId = getIntOrNull(input, "vatAccountId");
        Integer expenseRevenueAccountId = getIntOrNull(input, "expenseRevenueAccountId");

        if (counterpartyAccountId != null || vatAccountId != null || expenseRevenueAccountId != null) {
            RecognizedInvoice.SuggestedAccounts suggested = new RecognizedInvoice.SuggestedAccounts();

            if (counterpartyAccountId != null) {
                accountRepository.findById(counterpartyAccountId).ifPresent(acc ->
                        suggested.setCounterpartyAccount(new RecognizedInvoice.AccountInfo(acc.getId(), acc.getCode(), acc.getName())));
            }
            if (vatAccountId != null) {
                accountRepository.findById(vatAccountId).ifPresent(acc ->
                        suggested.setVatAccount(new RecognizedInvoice.AccountInfo(acc.getId(), acc.getCode(), acc.getName())));
            }
            if (expenseRevenueAccountId != null) {
                accountRepository.findById(expenseRevenueAccountId).ifPresent(acc ->
                        suggested.setExpenseOrRevenueAccount(new RecognizedInvoice.AccountInfo(acc.getId(), acc.getCode(), acc.getName())));
            }

            invoice.setSuggestedAccounts(suggested);
        }

        return invoice;
    }

    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof String) return LocalDate.parse((String) value);
        return null;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) return new BigDecimal((String) value);
        return null;
    }

    private Integer getIntOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return null;
    }
}
