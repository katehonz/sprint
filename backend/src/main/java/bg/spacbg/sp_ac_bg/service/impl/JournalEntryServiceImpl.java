package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateEntryLineInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.JournalEntryFilter;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.output.JournalEntriesPage;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity.ProcessingStatus;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.AccountingPeriodService;
import bg.spacbg.sp_ac_bg.service.JournalEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JournalEntryServiceImpl implements JournalEntryService {

    private static final Logger log = LoggerFactory.getLogger(JournalEntryServiceImpl.class);

    private final JournalEntryRepository journalEntryRepository;
    private final EntryLineRepository entryLineRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final CounterpartRepository counterpartRepository;
    private final UserRepository userRepository;
    private final ScannedInvoiceRepository scannedInvoiceRepository;
    private final AccountingPeriodService accountingPeriodService;

    public JournalEntryServiceImpl(
            JournalEntryRepository journalEntryRepository,
            EntryLineRepository entryLineRepository,
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            CounterpartRepository counterpartRepository,
            UserRepository userRepository,
            ScannedInvoiceRepository scannedInvoiceRepository,
            AccountingPeriodService accountingPeriodService) {
        this.journalEntryRepository = journalEntryRepository;
        this.entryLineRepository = entryLineRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.counterpartRepository = counterpartRepository;
        this.userRepository = userRepository;
        this.scannedInvoiceRepository = scannedInvoiceRepository;
        this.accountingPeriodService = accountingPeriodService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryEntity> findByFilter(JournalEntryFilter filter) {
        Specification<JournalEntryEntity> spec = buildSpecification(filter);
        return journalEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "accountingDate", "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntriesPage findByFilterPaged(JournalEntryFilter filter) {
        Specification<JournalEntryEntity> spec = buildSpecification(filter);

        int offset = filter.getOffset() != null ? filter.getOffset() : 0;
        int limit = filter.getLimit() != null ? filter.getLimit() : 50;

        // Get total count
        long totalCount = journalEntryRepository.count(spec);

        // Get paginated results
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "accountingDate", "id"));
        Page<JournalEntryEntity> resultPage = journalEntryRepository.findAll(spec, pageable);

        boolean hasMore = (offset + limit) < totalCount;

        return new JournalEntriesPage(resultPage.getContent(), totalCount, hasMore);
    }

    private Specification<JournalEntryEntity> buildSpecification(JournalEntryFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Company filter (required)
            predicates.add(cb.equal(root.get("company").get("id"), filter.getCompanyId()));

            // Date range filter
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("accountingDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("accountingDate"), filter.getToDate()));
            }

            // Posted status filter
            if (filter.getIsPosted() != null) {
                predicates.add(cb.equal(root.get("isPosted"), filter.getIsPosted()));
            }

            // Document number filter
            if (filter.getDocumentNumber() != null && !filter.getDocumentNumber().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("documentNumber")),
                        "%" + filter.getDocumentNumber().toLowerCase() + "%"));
            }

            // Text search (description, entryNumber, documentNumber)
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchLower = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate descSearch = cb.like(cb.lower(root.get("description")), searchLower);
                Predicate entryNumSearch = cb.like(cb.lower(root.get("entryNumber")), searchLower);
                Predicate docNumSearch = cb.like(cb.lower(root.get("documentNumber")), searchLower);
                predicates.add(cb.or(descSearch, entryNumSearch, docNumSearch));
            }

            // VAT date range filter
            if (filter.getVatFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("vatDate"), filter.getVatFromDate()));
            }
            if (filter.getVatToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("vatDate"), filter.getVatToDate()));
            }

            // VAT operation filters
            if (filter.getVatPurchaseOperation() != null && !filter.getVatPurchaseOperation().isEmpty()) {
                predicates.add(cb.isNotNull(root.get("vatPurchaseOperation")));
            }
            if (filter.getVatSalesOperation() != null && !filter.getVatSalesOperation().isEmpty()) {
                predicates.add(cb.isNotNull(root.get("vatSalesOperation")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryEntity> findUnposted(Integer companyId) {
        return journalEntryRepository.findByCompany_IdAndIsPostedFalse(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalEntryEntity> findById(Integer id) {
        return journalEntryRepository.findById(id);
    }

    @Override
    public JournalEntryEntity create(CreateJournalEntryInput input, Integer userId) {
        // Validate that the accounting period is open
        accountingPeriodService.validatePeriodIsOpen(input.getCompanyId(), input.getAccountingDate());

        // Validate debit = credit
        validateBalance(input.getLines());

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен: " + userId));

        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setEntryNumber(input.getEntryNumber() != null ? input.getEntryNumber() : generateEntryNumber(input.getCompanyId()));
        entry.setDocumentDate(input.getDocumentDate());
        entry.setVatDate(input.getVatDate());
        entry.setAccountingDate(input.getAccountingDate());
        entry.setDocumentNumber(input.getDocumentNumber());
        entry.setDescription(input.getDescription());
        entry.setCompany(company);
        entry.setCreatedBy(user);
        entry.setPosted(false);
        entry.setDocumentType(input.getDocumentType());
        entry.setVatDocumentType(input.getVatDocumentType());
        entry.setVatPurchaseOperation(input.getVatPurchaseOperation());
        entry.setVatSalesOperation(input.getVatSalesOperation());

        // Set counterpart if provided
        if (input.getCounterpartId() != null) {
            CounterpartEntity counterpart = counterpartRepository.findById(input.getCounterpartId())
                    .orElseThrow(() -> new IllegalArgumentException("Контрагентът не е намерен: " + input.getCounterpartId()));
            entry.setCounterpart(counterpart);
        }

        // Use provided totals or calculate from lines
        if (input.getTotalAmount() != null) {
            entry.setTotalAmount(input.getTotalAmount());
        } else {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (CreateEntryLineInput line : input.getLines()) {
                BigDecimal lineAmount = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(lineAmount);
            }
            entry.setTotalAmount(totalAmount);
        }

        if (input.getTotalVatAmount() != null) {
            entry.setTotalVatAmount(input.getTotalVatAmount());
        } else {
            BigDecimal totalVatAmount = BigDecimal.ZERO;
            for (CreateEntryLineInput line : input.getLines()) {
                if (line.getVatAmount() != null) {
                    totalVatAmount = totalVatAmount.add(line.getVatAmount());
                }
            }
            entry.setTotalVatAmount(totalVatAmount);
        }

        JournalEntryEntity savedEntry = journalEntryRepository.save(entry);

        // Create entry lines
        List<EntryLineEntity> lines = createEntryLines(savedEntry, input.getLines());
        savedEntry.setEntryLines(lines);

        // Link scanned invoice if provided
        if (input.getScannedInvoiceId() != null) {
            scannedInvoiceRepository.findById(input.getScannedInvoiceId()).ifPresent(scannedInvoice -> {
                scannedInvoice.setJournalEntry(savedEntry);
                scannedInvoice.setStatus(ProcessingStatus.PROCESSED);
                scannedInvoiceRepository.save(scannedInvoice);
                log.info("Linked journal entry {} to scanned invoice {}", savedEntry.getEntryNumber(), input.getScannedInvoiceId());
            });
        }

        return savedEntry;
    }

    @Override
    public JournalEntryEntity update(Integer id, UpdateJournalEntryInput input) {
        JournalEntryEntity entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Счетоводната статия не е намерена: " + id));

        if (entry.isPosted()) {
            throw new IllegalStateException("Не може да се редактира осчетоводена статия");
        }

        // Validate that the current accounting period is open
        accountingPeriodService.validatePeriodIsOpen(entry.getCompany().getId(), entry.getAccountingDate());

        // If changing accounting date, validate the new period is also open
        if (input.getAccountingDate() != null && !input.getAccountingDate().equals(entry.getAccountingDate())) {
            accountingPeriodService.validatePeriodIsOpen(entry.getCompany().getId(), input.getAccountingDate());
        }

        if (input.getDocumentDate() != null) entry.setDocumentDate(input.getDocumentDate());
        if (input.getVatDate() != null) entry.setVatDate(input.getVatDate());
        if (input.getAccountingDate() != null) entry.setAccountingDate(input.getAccountingDate());
        if (input.getDocumentNumber() != null) entry.setDocumentNumber(input.getDocumentNumber());
        if (input.getDescription() != null) entry.setDescription(input.getDescription());
        if (input.getDocumentType() != null) entry.setDocumentType(input.getDocumentType());
        if (input.getVatDocumentType() != null) entry.setVatDocumentType(input.getVatDocumentType());

        if (input.getLines() != null) {
            validateBalance(input.getLines());
            entryLineRepository.deleteByJournalEntry_Id(id);
            List<EntryLineEntity> lines = createEntryLines(entry, input.getLines());
            entry.setEntryLines(lines);

            // Recalculate totals
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalVatAmount = BigDecimal.ZERO;
            for (CreateEntryLineInput line : input.getLines()) {
                BigDecimal lineAmount = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(lineAmount);
                if (line.getVatAmount() != null) {
                    totalVatAmount = totalVatAmount.add(line.getVatAmount());
                }
            }
            entry.setTotalAmount(totalAmount);
            entry.setTotalVatAmount(totalVatAmount);
        }

        return journalEntryRepository.save(entry);
    }

    @Override
    public boolean delete(Integer id) {
        Optional<JournalEntryEntity> entryOpt = journalEntryRepository.findById(id);
        if (entryOpt.isEmpty()) {
            return false;
        }
        JournalEntryEntity entry = entryOpt.get();
        if (entry.isPosted()) {
            throw new IllegalStateException("Не може да се изтрие осчетоводена статия");
        }
        // Validate that the accounting period is open
        accountingPeriodService.validatePeriodIsOpen(entry.getCompany().getId(), entry.getAccountingDate());

        journalEntryRepository.deleteById(id);
        return true;
    }

    @Override
    public JournalEntryEntity post(Integer id, Integer userId) {
        JournalEntryEntity entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Счетоводната статия не е намерена: " + id));

        if (entry.isPosted()) {
            throw new IllegalStateException("Статията вече е осчетоводена");
        }

        // Validate that the accounting period is open
        accountingPeriodService.validatePeriodIsOpen(entry.getCompany().getId(), entry.getAccountingDate());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен: " + userId));

        entry.setPosted(true);
        entry.setPostedBy(user);
        entry.setPostedAt(OffsetDateTime.now());

        return journalEntryRepository.save(entry);
    }

    @Override
    public JournalEntryEntity unpost(Integer id) {
        JournalEntryEntity entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Счетоводната статия не е намерена: " + id));

        if (!entry.isPosted()) {
            throw new IllegalStateException("Статията не е осчетоводена");
        }

        // Validate that the accounting period is open
        accountingPeriodService.validatePeriodIsOpen(entry.getCompany().getId(), entry.getAccountingDate());

        entry.setPosted(false);
        entry.setPostedBy(null);
        entry.setPostedAt(null);

        return journalEntryRepository.save(entry);
    }

    @Override
    public String generateEntryNumber(Integer companyId) {
        String prefix = "JE-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer maxNum = journalEntryRepository.findMaxEntryNumberByPrefix(companyId, prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%04d", nextNum);
    }

    private void validateBalance(List<CreateEntryLineInput> lines) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (CreateEntryLineInput line : lines) {
            if (line.getDebitAmount() != null) {
                totalDebit = totalDebit.add(line.getDebitAmount());
            }
            if (line.getCreditAmount() != null) {
                totalCredit = totalCredit.add(line.getCreditAmount());
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    String.format("Дебит (%s) не е равен на кредит (%s)", totalDebit, totalCredit));
        }
    }

    private List<EntryLineEntity> createEntryLines(JournalEntryEntity entry, List<CreateEntryLineInput> lineInputs) {
        List<EntryLineEntity> lines = new ArrayList<>();
        int order = 1;

        for (CreateEntryLineInput input : lineInputs) {
            EntryLineEntity line = new EntryLineEntity();
            line.setJournalEntry(entry);

            AccountEntity account = accountRepository.findById(input.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Сметката не е намерена: " + input.getAccountId()));
            line.setAccount(account);

            line.setDebitAmount(input.getDebitAmount() != null ? input.getDebitAmount() : BigDecimal.ZERO);
            line.setCreditAmount(input.getCreditAmount() != null ? input.getCreditAmount() : BigDecimal.ZERO);

            if (input.getCounterpartId() != null) {
                CounterpartEntity counterpart = counterpartRepository.findById(input.getCounterpartId())
                        .orElseThrow(() -> new IllegalArgumentException("Контрагентът не е намерен: " + input.getCounterpartId()));
                line.setCounterpart(counterpart);
            }

            line.setCurrencyCode(input.getCurrencyCode() != null ? input.getCurrencyCode() : "BGN");
            line.setExchangeRate(input.getExchangeRate() != null ? input.getExchangeRate() : BigDecimal.ONE);

            // Calculate base amount
            BigDecimal amount = line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                    ? line.getDebitAmount()
                    : line.getCreditAmount();
            line.setBaseAmount(amount);

            line.setVatAmount(input.getVatAmount() != null ? input.getVatAmount() : BigDecimal.ZERO);
            line.setQuantity(input.getQuantity());
            line.setUnitOfMeasureCode(input.getUnitOfMeasureCode());
            line.setDescription(input.getDescription());
            line.setLineOrder(input.getLineOrder() != null ? input.getLineOrder() : order++);

            lines.add(entryLineRepository.save(line));
        }

        return lines;
    }
}
