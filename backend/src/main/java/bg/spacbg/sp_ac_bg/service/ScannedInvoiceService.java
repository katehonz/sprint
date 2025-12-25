package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice;
import bg.spacbg.sp_ac_bg.model.dto.ViesValidationResult;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateEntryLineInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity.*;
import bg.spacbg.sp_ac_bg.model.entity.UserEntity;
import bg.spacbg.sp_ac_bg.repository.AccountRepository;
import bg.spacbg.sp_ac_bg.repository.ScannedInvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ScannedInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(ScannedInvoiceService.class);

    private final ScannedInvoiceRepository scannedInvoiceRepository;
    private final AccountRepository accountRepository;
    private final ViesService viesService;
    private final JournalEntryService journalEntryService;

    public ScannedInvoiceService(ScannedInvoiceRepository scannedInvoiceRepository,
                                  AccountRepository accountRepository,
                                  ViesService viesService,
                                  JournalEntryService journalEntryService) {
        this.scannedInvoiceRepository = scannedInvoiceRepository;
        this.accountRepository = accountRepository;
        this.viesService = viesService;
        this.journalEntryService = journalEntryService;
    }

    /**
     * Save a recognized invoice to the database.
     */
    @Transactional
    public ScannedInvoiceEntity saveFromRecognized(RecognizedInvoice recognized, CompanyEntity company,
                                                    UserEntity user, String originalFileName) {
        ScannedInvoiceEntity entity = new ScannedInvoiceEntity();

        entity.setCompany(company);
        entity.setCreatedBy(user);
        entity.setOriginalFileName(originalFileName);

        // Set direction
        entity.setDirection(toEntityDirection(recognized.getDirection()));

        // Set status
        entity.setStatus(recognized.isRequiresManualReview() ? ProcessingStatus.PENDING : ProcessingStatus.PENDING);

        // Vendor info
        entity.setVendorName(recognized.getVendorName());
        entity.setVendorVatNumber(recognized.getVendorVatNumber());
        entity.setVendorAddress(recognized.getVendorAddress());

        // Customer info
        entity.setCustomerName(recognized.getCustomerName());
        entity.setCustomerVatNumber(recognized.getCustomerVatNumber());
        entity.setCustomerAddress(recognized.getCustomerAddress());

        // Invoice details
        entity.setInvoiceNumber(recognized.getInvoiceId());
        entity.setInvoiceDate(recognized.getInvoiceDate());
        entity.setDueDate(recognized.getDueDate());

        // Amounts
        entity.setSubtotal(recognized.getSubtotal());
        entity.setTotalTax(recognized.getTotalTax());
        entity.setInvoiceTotal(recognized.getInvoiceTotal());

        // VIES status
        entity.setViesStatus(toEntityViesStatus(recognized.getValidationStatus()));
        entity.setViesValidationMessage(recognized.getViesValidationMessage());

        // Manual review
        entity.setRequiresManualReview(recognized.isRequiresManualReview());
        entity.setManualReviewReason(recognized.getManualReviewReason());

        // Set suggested accounts
        if (recognized.getSuggestedAccounts() != null) {
            var suggested = recognized.getSuggestedAccounts();
            if (suggested.getCounterpartyAccount() != null) {
                accountRepository.findById(suggested.getCounterpartyAccount().getId())
                        .ifPresent(entity::setCounterpartyAccount);
            }
            if (suggested.getVatAccount() != null) {
                accountRepository.findById(suggested.getVatAccount().getId())
                        .ifPresent(entity::setVatAccount);
            }
            if (suggested.getExpenseOrRevenueAccount() != null) {
                accountRepository.findById(suggested.getExpenseOrRevenueAccount().getId())
                        .ifPresent(entity::setExpenseRevenueAccount);
            }
        }

        return scannedInvoiceRepository.save(entity);
    }

    /**
     * Get all scanned invoices for a company.
     */
    public List<ScannedInvoiceEntity> findByCompany(Integer companyId) {
        return scannedInvoiceRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /**
     * Get scanned invoices by direction (purchases or sales).
     */
    public List<ScannedInvoiceEntity> findByCompanyAndDirection(Integer companyId, InvoiceDirection direction) {
        return scannedInvoiceRepository.findByCompanyIdAndDirectionOrderByCreatedAtDesc(companyId, direction);
    }

    /**
     * Get a scanned invoice by ID.
     */
    public Optional<ScannedInvoiceEntity> findById(Integer id) {
        return scannedInvoiceRepository.findById(id);
    }

    /**
     * Update accounts for a scanned invoice.
     */
    @Transactional
    public ScannedInvoiceEntity updateAccounts(Integer id, Integer counterpartyAccountId,
                                                Integer vatAccountId, Integer expenseRevenueAccountId) {
        ScannedInvoiceEntity entity = scannedInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scanned invoice not found: " + id));

        if (counterpartyAccountId != null) {
            entity.setCounterpartyAccount(accountRepository.findById(counterpartyAccountId).orElse(null));
        }
        if (vatAccountId != null) {
            entity.setVatAccount(accountRepository.findById(vatAccountId).orElse(null));
        }
        if (expenseRevenueAccountId != null) {
            entity.setExpenseRevenueAccount(accountRepository.findById(expenseRevenueAccountId).orElse(null));
        }

        return scannedInvoiceRepository.save(entity);
    }

    /**
     * Validate counterparty VAT number via VIES.
     */
    @Transactional
    public ScannedInvoiceEntity validateVies(Integer id) {
        ScannedInvoiceEntity entity = scannedInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scanned invoice not found: " + id));

        // Determine which VAT number to validate
        String vatToValidate;
        if (entity.getDirection() == InvoiceDirection.PURCHASE) {
            vatToValidate = entity.getVendorVatNumber();
        } else {
            vatToValidate = entity.getCustomerVatNumber();
        }

        if (vatToValidate == null || vatToValidate.isBlank()) {
            entity.setViesStatus(ViesStatus.NOT_APPLICABLE);
            entity.setViesValidationMessage("Няма ДДС номер за валидация");
            return scannedInvoiceRepository.save(entity);
        }

        // Validate via VIES using the working service
        ViesValidationResult result = viesService.validateVat(vatToValidate);

        if (result.isValid()) {
            entity.setViesStatus(ViesStatus.VALID);
            entity.setViesValidationMessage("ДДС номерът е валиден (" + result.getSource() + ")");
            entity.setViesCompanyName(result.getName());
            entity.setViesCompanyAddress(result.getLongAddress());
            entity.setStatus(ProcessingStatus.VALIDATED);
        } else {
            entity.setViesStatus(ViesStatus.INVALID);
            entity.setViesValidationMessage(result.getErrorMessage() != null ? result.getErrorMessage() : "ДДС номерът не е валиден");
        }
        entity.setViesValidatedAt(OffsetDateTime.now());

        return scannedInvoiceRepository.save(entity);
    }

    /**
     * Delete a scanned invoice.
     */
    @Transactional
    public void delete(Integer id) {
        scannedInvoiceRepository.deleteById(id);
    }

    /**
     * Mark invoice as rejected.
     */
    @Transactional
    public ScannedInvoiceEntity reject(Integer id, String reason) {
        ScannedInvoiceEntity entity = scannedInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scanned invoice not found: " + id));
        entity.setStatus(ProcessingStatus.REJECTED);
        entity.setNotes(reason);
        return scannedInvoiceRepository.save(entity);
    }

    private InvoiceDirection toEntityDirection(RecognizedInvoice.InvoiceDirection direction) {
        if (direction == null) return InvoiceDirection.PURCHASE;
        return switch (direction) {
            case PURCHASE -> InvoiceDirection.PURCHASE;
            case SALE -> InvoiceDirection.SALE;
            case UNKNOWN -> InvoiceDirection.PURCHASE; // Default to purchase
        };
    }

    private ViesStatus toEntityViesStatus(RecognizedInvoice.ValidationStatus status) {
        if (status == null) return ViesStatus.PENDING;
        return switch (status) {
            case PENDING -> ViesStatus.PENDING;
            case VALID -> ViesStatus.VALID;
            case INVALID -> ViesStatus.INVALID;
            case NOT_APPLICABLE -> ViesStatus.NOT_APPLICABLE;
            case MANUAL_REVIEW -> ViesStatus.PENDING;
        };
    }

    private ViesStatus toEntityViesStatus(String status) {
        if (status == null) return ViesStatus.PENDING;
        return switch (status) {
            case "VALID" -> ViesStatus.VALID;
            case "INVALID" -> ViesStatus.INVALID;
            case "NOT_APPLICABLE" -> ViesStatus.NOT_APPLICABLE;
            case "ERROR" -> ViesStatus.ERROR;
            default -> ViesStatus.PENDING;
        };
    }

    /**
     * Process a scanned invoice and create a journal entry.
     *
     * For PURCHASE:
     * - Debit: Expense/Revenue account (subtotal) + VAT account (totalTax)
     * - Credit: Counterparty account (invoiceTotal)
     *
     * For SALE:
     * - Debit: Counterparty account (invoiceTotal)
     * - Credit: Expense/Revenue account (subtotal) + VAT account (totalTax)
     */
    @Transactional
    public ScannedInvoiceEntity processToJournalEntry(Integer id, Integer userId) {
        ScannedInvoiceEntity invoice = scannedInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сканираната фактура не е намерена: " + id));

        // Validate status
        if (invoice.getStatus() == ProcessingStatus.PROCESSED) {
            throw new IllegalStateException("Фактурата вече е осчетоводена");
        }
        if (invoice.getStatus() == ProcessingStatus.REJECTED) {
            throw new IllegalStateException("Фактурата е отхвърлена и не може да бъде осчетоводена");
        }

        // Validate required accounts
        if (invoice.getCounterpartyAccount() == null) {
            throw new IllegalArgumentException("Не е избрана сметка за контрагент (401/411)");
        }
        if (invoice.getExpenseRevenueAccount() == null) {
            throw new IllegalArgumentException("Не е избрана сметка за разход/приход");
        }

        // Validate amounts
        BigDecimal subtotal = invoice.getSubtotal();
        BigDecimal totalTax = invoice.getTotalTax();
        BigDecimal invoiceTotal = invoice.getInvoiceTotal();

        if (invoiceTotal == null || invoiceTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Невалидна обща сума на фактурата");
        }

        // If subtotal is missing, calculate it
        if (subtotal == null) {
            if (totalTax != null) {
                subtotal = invoiceTotal.subtract(totalTax);
            } else {
                subtotal = invoiceTotal;
                totalTax = BigDecimal.ZERO;
            }
        }
        if (totalTax == null) {
            totalTax = BigDecimal.ZERO;
        }

        // Build description
        String counterpartyName = invoice.getDirection() == InvoiceDirection.PURCHASE
                ? invoice.getVendorName()
                : invoice.getCustomerName();
        String description = String.format("Фактура %s от %s",
                invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "б/н",
                counterpartyName != null ? counterpartyName : "неизвестен");

        // Create journal entry input
        CreateJournalEntryInput entryInput = new CreateJournalEntryInput();
        entryInput.setCompanyId(invoice.getCompany().getId());
        entryInput.setDocumentDate(invoice.getInvoiceDate());
        entryInput.setVatDate(invoice.getInvoiceDate());
        entryInput.setAccountingDate(invoice.getInvoiceDate());
        entryInput.setDocumentNumber(invoice.getInvoiceNumber());
        entryInput.setDescription(description);
        entryInput.setTotalAmount(invoiceTotal);
        entryInput.setTotalVatAmount(totalTax);

        // Set VAT document type and operation
        if (invoice.getDirection() == InvoiceDirection.PURCHASE) {
            entryInput.setVatDocumentType("01"); // Фактура
            entryInput.setVatPurchaseOperation("01"); // ВОП с право на пълен данъчен кредит
        } else {
            entryInput.setVatDocumentType("01"); // Фактура
            entryInput.setVatSalesOperation("01"); // Облагаема доставка 20%
        }

        // Create entry lines
        List<CreateEntryLineInput> lines = new ArrayList<>();
        int lineOrder = 1;

        if (invoice.getDirection() == InvoiceDirection.PURCHASE) {
            // PURCHASE: Debit expense + VAT, Credit supplier

            // Line 1: Debit expense account (subtotal without VAT)
            CreateEntryLineInput expenseLine = new CreateEntryLineInput();
            expenseLine.setAccountId(invoice.getExpenseRevenueAccount().getId());
            expenseLine.setDebitAmount(subtotal);
            expenseLine.setCreditAmount(BigDecimal.ZERO);
            expenseLine.setDescription("Разход по фактура " + invoice.getInvoiceNumber());
            expenseLine.setLineOrder(lineOrder++);
            lines.add(expenseLine);

            // Line 2: Debit VAT account (if VAT > 0 and VAT account is set)
            if (totalTax.compareTo(BigDecimal.ZERO) > 0 && invoice.getVatAccount() != null) {
                CreateEntryLineInput vatLine = new CreateEntryLineInput();
                vatLine.setAccountId(invoice.getVatAccount().getId());
                vatLine.setDebitAmount(totalTax);
                vatLine.setCreditAmount(BigDecimal.ZERO);
                vatLine.setVatAmount(totalTax);
                vatLine.setDescription("ДДС по фактура " + invoice.getInvoiceNumber());
                vatLine.setLineOrder(lineOrder++);
                lines.add(vatLine);
            } else if (totalTax.compareTo(BigDecimal.ZERO) > 0) {
                // VAT exists but no VAT account - add to expense
                expenseLine.setDebitAmount(invoiceTotal);
            }

            // Line 3: Credit supplier account (total amount)
            CreateEntryLineInput supplierLine = new CreateEntryLineInput();
            supplierLine.setAccountId(invoice.getCounterpartyAccount().getId());
            supplierLine.setDebitAmount(BigDecimal.ZERO);
            supplierLine.setCreditAmount(invoiceTotal);
            supplierLine.setDescription("Задължение към " + counterpartyName);
            supplierLine.setLineOrder(lineOrder++);
            lines.add(supplierLine);

        } else {
            // SALE: Debit customer, Credit revenue + VAT

            // Line 1: Debit customer account (total amount)
            CreateEntryLineInput customerLine = new CreateEntryLineInput();
            customerLine.setAccountId(invoice.getCounterpartyAccount().getId());
            customerLine.setDebitAmount(invoiceTotal);
            customerLine.setCreditAmount(BigDecimal.ZERO);
            customerLine.setDescription("Вземане от " + counterpartyName);
            customerLine.setLineOrder(lineOrder++);
            lines.add(customerLine);

            // Line 2: Credit revenue account (subtotal without VAT)
            CreateEntryLineInput revenueLine = new CreateEntryLineInput();
            revenueLine.setAccountId(invoice.getExpenseRevenueAccount().getId());
            revenueLine.setDebitAmount(BigDecimal.ZERO);
            revenueLine.setCreditAmount(subtotal);
            revenueLine.setDescription("Приход по фактура " + invoice.getInvoiceNumber());
            revenueLine.setLineOrder(lineOrder++);
            lines.add(revenueLine);

            // Line 3: Credit VAT account (if VAT > 0 and VAT account is set)
            if (totalTax.compareTo(BigDecimal.ZERO) > 0 && invoice.getVatAccount() != null) {
                CreateEntryLineInput vatLine = new CreateEntryLineInput();
                vatLine.setAccountId(invoice.getVatAccount().getId());
                vatLine.setDebitAmount(BigDecimal.ZERO);
                vatLine.setCreditAmount(totalTax);
                vatLine.setVatAmount(totalTax);
                vatLine.setDescription("ДДС по фактура " + invoice.getInvoiceNumber());
                vatLine.setLineOrder(lineOrder++);
                lines.add(vatLine);
            } else if (totalTax.compareTo(BigDecimal.ZERO) > 0) {
                // VAT exists but no VAT account - add to revenue
                revenueLine.setCreditAmount(invoiceTotal);
            }
        }

        entryInput.setLines(lines);

        // Create the journal entry
        JournalEntryEntity journalEntry = journalEntryService.create(entryInput, userId);

        // Update scanned invoice with reference to journal entry
        invoice.setJournalEntry(journalEntry);
        invoice.setStatus(ProcessingStatus.PROCESSED);

        log.info("Created journal entry {} for scanned invoice {}", journalEntry.getEntryNumber(), id);

        return scannedInvoiceRepository.save(invoice);
    }
}
