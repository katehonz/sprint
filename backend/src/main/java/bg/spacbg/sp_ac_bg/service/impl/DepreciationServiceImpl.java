package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.DepreciationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DepreciationService
 * Handles fixed asset depreciation calculations according to Bulgarian accounting
 * and tax standards (ЗКПО - Corporate Income Tax Act)
 */
@Service
@Transactional
public class DepreciationServiceImpl implements DepreciationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String DEPRECIATION_METHOD_LINEAR = "LINEAR";
    private static final String DEPRECIATION_METHOD_DECLINING = "DECLINING_BALANCE";

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetCategoryRepository categoryRepository;
    private final DepreciationJournalRepository depreciationJournalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final EntryLineRepository entryLineRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public DepreciationServiceImpl(
            FixedAssetRepository fixedAssetRepository,
            FixedAssetCategoryRepository categoryRepository,
            DepreciationJournalRepository depreciationJournalRepository,
            JournalEntryRepository journalEntryRepository,
            EntryLineRepository entryLineRepository,
            AccountRepository accountRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository) {
        this.fixedAssetRepository = fixedAssetRepository;
        this.categoryRepository = categoryRepository;
        this.depreciationJournalRepository = depreciationJournalRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.entryLineRepository = entryLineRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DepreciationJournalEntity calculateMonthlyDepreciation(Integer assetId, LocalDate period) {
        // Normalize period to first day of month
        LocalDate normalizedPeriod = period.withDayOfMonth(1);

        FixedAssetEntity asset = fixedAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Дълготрайният актив не е намерен: " + assetId));

        CompanyEntity company = asset.getCompany();

        // Validate asset is active
        if (!STATUS_ACTIVE.equals(asset.getStatus())) {
            throw new IllegalStateException("Активът " + asset.getName() + " не е активен");
        }

        // Check if depreciation already calculated for this period
        if (depreciationJournalRepository.existsByFixedAssetIdAndPeriod(assetId, normalizedPeriod)) {
            throw new IllegalStateException(String.format(
                    "Амортизацията за актив %s вече е изчислена за период %d-%02d",
                    asset.getName(), normalizedPeriod.getYear(), normalizedPeriod.getMonthValue()));
        }

        // Check if asset was put into service before or during this period
        if (asset.getPutIntoServiceDate() == null) {
            throw new IllegalStateException("Активът " + asset.getName() + " няма дата на въвеждане в експлоатация");
        }

        if (asset.getPutIntoServiceDate().isAfter(normalizedPeriod.plusMonths(1).minusDays(1))) {
            throw new IllegalStateException(String.format(
                    "Активът %s не е въведен в експлоатация за период %d-%02d",
                    asset.getName(), normalizedPeriod.getYear(), normalizedPeriod.getMonthValue()));
        }

        // Validate sequential period calculation
        validateSequentialPeriod(asset, normalizedPeriod);

        // Calculate accounting depreciation
        BigDecimal accountingMonthly = calculateAccountingDepreciation(asset);
        BigDecimal accountingBookValueBefore = asset.getAccountingBookValue();
        BigDecimal accountingBookValueAfter = accountingBookValueBefore.subtract(accountingMonthly)
                .max(asset.getAccountingSalvageValue());
        BigDecimal actualAccountingAmount = accountingBookValueBefore.subtract(accountingBookValueAfter);

        // Calculate tax depreciation
        BigDecimal taxMonthly = calculateTaxDepreciation(asset);
        BigDecimal taxBookValueBefore = asset.getTaxBookValue();
        BigDecimal taxBookValueAfter = taxBookValueBefore.subtract(taxMonthly).max(BigDecimal.ZERO);
        BigDecimal actualTaxAmount = taxBookValueBefore.subtract(taxBookValueAfter);

        // Create depreciation journal entry
        DepreciationJournalEntity depreciationJournal = new DepreciationJournalEntity();
        depreciationJournal.setFixedAsset(asset);
        depreciationJournal.setPeriod(normalizedPeriod);
        depreciationJournal.setCompany(company);
        depreciationJournal.setAccountingDepreciationAmount(actualAccountingAmount);
        depreciationJournal.setAccountingBookValueBefore(accountingBookValueBefore);
        depreciationJournal.setAccountingBookValueAfter(accountingBookValueAfter);
        depreciationJournal.setTaxDepreciationAmount(actualTaxAmount);
        depreciationJournal.setTaxBookValueBefore(taxBookValueBefore);
        depreciationJournal.setTaxBookValueAfter(taxBookValueAfter);
        depreciationJournal.setPosted(false);

        DepreciationJournalEntity saved = depreciationJournalRepository.save(depreciationJournal);

        // Update fixed asset book values
        asset.setAccountingBookValue(accountingBookValueAfter);
        asset.setAccountingAccumulatedDepreciation(
                asset.getAccountingAccumulatedDepreciation().add(actualAccountingAmount));
        asset.setTaxBookValue(taxBookValueAfter);
        asset.setTaxAccumulatedDepreciation(
                asset.getTaxAccumulatedDepreciation().add(actualTaxAmount));
        fixedAssetRepository.save(asset);

        return saved;
    }

    @Override
    public DepreciationCalculationResult calculateBulkDepreciation(Integer companyId, LocalDate period) {
        LocalDate normalizedPeriod = period.withDayOfMonth(1);

        List<FixedAssetEntity> assets = getAssetsNeedingDepreciation(companyId, normalizedPeriod);

        List<DepreciationJournalEntity> calculated = new ArrayList<>();
        List<DepreciationError> errors = new ArrayList<>();
        BigDecimal totalAccountingAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;

        for (FixedAssetEntity asset : assets) {
            try {
                DepreciationJournalEntity depreciation = calculateMonthlyDepreciation(asset.getId(), normalizedPeriod);
                calculated.add(depreciation);
                totalAccountingAmount = totalAccountingAmount.add(depreciation.getAccountingDepreciationAmount());
                totalTaxAmount = totalTaxAmount.add(depreciation.getTaxDepreciationAmount());
            } catch (Exception e) {
                errors.add(new DepreciationError(asset.getId(), asset.getName(), e.getMessage()));
            }
        }

        return new DepreciationCalculationResult(calculated, errors, totalAccountingAmount, totalTaxAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepreciationJournalEntity> getDepreciationJournal(Integer companyId, Integer year, Integer month) {
        if (month != null) {
            return depreciationJournalRepository.findByCompanyIdAndYearAndMonth(companyId, year, month);
        }
        return depreciationJournalRepository.findByCompanyIdAndYear(companyId, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FixedAssetEntity> getAssetsNeedingDepreciation(Integer companyId, LocalDate period) {
        LocalDate normalizedPeriod = period.withDayOfMonth(1);

        // Get all active assets for the company that were put into service before or during this period
        List<FixedAssetEntity> activeAssets = fixedAssetRepository.findActiveAssetsForDepreciation(
                companyId, normalizedPeriod.plusMonths(1).minusDays(1));

        // Filter out assets that already have depreciation for this period
        // and assets that are fully depreciated
        return activeAssets.stream()
                .filter(asset -> !depreciationJournalRepository.existsByFixedAssetIdAndPeriod(
                        asset.getId(), normalizedPeriod))
                .filter(asset -> asset.getAccountingBookValue().compareTo(asset.getAccountingSalvageValue()) > 0
                        || asset.getTaxBookValue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalculatedPeriod> getCalculatedPeriods(Integer companyId) {
        List<LocalDate> periods = depreciationJournalRepository.findDistinctPeriodsByCompanyId(companyId);
        List<LocalDate> postedPeriods = depreciationJournalRepository.findDistinctPostedPeriodsByCompanyId(companyId);
        Set<LocalDate> postedSet = new HashSet<>(postedPeriods);

        List<CalculatedPeriod> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("bg"));

        for (LocalDate period : periods) {
            List<DepreciationJournalEntity> entries = depreciationJournalRepository
                    .findByCompanyIdAndPeriod(companyId, period);

            BigDecimal totalAccounting = entries.stream()
                    .map(DepreciationJournalEntity::getAccountingDepreciationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalTax = entries.stream()
                    .map(DepreciationJournalEntity::getTaxDepreciationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(new CalculatedPeriod(
                    period.getYear(),
                    period.getMonthValue(),
                    period.format(formatter),
                    postedSet.contains(period),
                    totalAccounting,
                    totalTax,
                    entries.size()
            ));
        }

        return result;
    }

    @Override
    public JournalEntryEntity postDepreciation(Integer companyId, LocalDate period, Integer userId) {
        LocalDate normalizedPeriod = period.withDayOfMonth(1);

        // Get unposted depreciation entries for the period
        List<DepreciationJournalEntity> unpostedEntries = depreciationJournalRepository
                .findByCompanyIdAndPeriodAndIsPostedFalse(companyId, normalizedPeriod);

        if (unpostedEntries.isEmpty()) {
            throw new IllegalStateException("Няма неосчетоводени амортизации за този период");
        }

        // Filter out zero amounts
        List<DepreciationJournalEntity> nonZeroEntries = unpostedEntries.stream()
                .filter(e -> e.getAccountingDepreciationAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (nonZeroEntries.isEmpty()) {
            throw new IllegalStateException("Всички амортизации за периода са с нулева стойност");
        }

        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + companyId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен: " + userId));

        // Group by category to get proper account codes
        Map<Integer, List<DepreciationJournalEntity>> byCategory = nonZeroEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getFixedAsset().getCategory().getId()));

        BigDecimal totalAmount = nonZeroEntries.stream()
                .map(DepreciationJournalEntity::getAccountingDepreciationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate entry number
        String entryNumber = String.format("DEP-%d-%02d-%s",
                normalizedPeriod.getYear(),
                normalizedPeriod.getMonthValue(),
                OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));

        // Create the journal entry
        JournalEntryEntity journalEntry = new JournalEntryEntity();
        journalEntry.setEntryNumber(entryNumber);
        journalEntry.setCompany(company);
        journalEntry.setDocumentDate(normalizedPeriod);
        journalEntry.setAccountingDate(normalizedPeriod);
        journalEntry.setDocumentNumber(String.format("Амортизация %d-%02d",
                normalizedPeriod.getYear(), normalizedPeriod.getMonthValue()));
        journalEntry.setDescription(String.format("Месечна амортизация за %d-%02d",
                normalizedPeriod.getYear(), normalizedPeriod.getMonthValue()));
        journalEntry.setDocumentType("DEPRECIATION");
        journalEntry.setTotalAmount(totalAmount);
        journalEntry.setTotalVatAmount(BigDecimal.ZERO);
        journalEntry.setPosted(true);
        journalEntry.setPostedAt(OffsetDateTime.now());

        JournalEntryEntity savedEntry = journalEntryRepository.save(journalEntry);

        // Create entry lines for each category
        int lineOrder = 1;
        for (Map.Entry<Integer, List<DepreciationJournalEntity>> categoryEntry : byCategory.entrySet()) {
            FixedAssetCategoryEntity category = categoryRepository.findById(categoryEntry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена"));

            BigDecimal categoryAmount = categoryEntry.getValue().stream()
                    .map(DepreciationJournalEntity::getAccountingDepreciationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Find accounts
            AccountEntity expenseAccount = accountRepository
                    .findByCompanyIdAndCode(companyId, category.getExpenseAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Сметка за разходи " + category.getExpenseAccountCode() + " не е намерена"));

            AccountEntity depreciationAccount = accountRepository
                    .findByCompanyIdAndCode(companyId, category.getDepreciationAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Сметка за амортизация " + category.getDepreciationAccountCode() + " не е намерена"));

            // Debit expense account (603)
            EntryLineEntity expenseLine = new EntryLineEntity();
            expenseLine.setJournalEntry(savedEntry);
            expenseLine.setAccount(expenseAccount);
            expenseLine.setDebitAmount(categoryAmount);
            expenseLine.setCreditAmount(BigDecimal.ZERO);
            expenseLine.setDescription(String.format("Амортизация %s - %s", category.getCode(), category.getName()));
            expenseLine.setLineOrder(lineOrder++);
            expenseLine.setBaseAmount(categoryAmount);
            expenseLine.setVatAmount(BigDecimal.ZERO);
            entryLineRepository.save(expenseLine);

            // Credit accumulated depreciation account (241)
            EntryLineEntity depreciationLine = new EntryLineEntity();
            depreciationLine.setJournalEntry(savedEntry);
            depreciationLine.setAccount(depreciationAccount);
            depreciationLine.setDebitAmount(BigDecimal.ZERO);
            depreciationLine.setCreditAmount(categoryAmount);
            depreciationLine.setDescription(String.format("Натрупана амортизация %s - %s",
                    category.getCode(), category.getName()));
            depreciationLine.setLineOrder(lineOrder++);
            depreciationLine.setBaseAmount(categoryAmount);
            depreciationLine.setVatAmount(BigDecimal.ZERO);
            entryLineRepository.save(depreciationLine);
        }

        // Mark all depreciation entries as posted
        OffsetDateTime now = OffsetDateTime.now();
        for (DepreciationJournalEntity depreciation : nonZeroEntries) {
            depreciation.setPosted(true);
            depreciation.setJournalEntry(savedEntry);
            depreciation.setPostedAt(now);
            depreciation.setPostedBy(user);
            depreciationJournalRepository.save(depreciation);
        }

        return savedEntry;
    }

    /**
     * Calculate accounting depreciation amount for a single month
     */
    private BigDecimal calculateAccountingDepreciation(FixedAssetEntity asset) {
        // If fully depreciated, return zero
        if (asset.getAccountingBookValue().compareTo(asset.getAccountingSalvageValue()) <= 0) {
            return BigDecimal.ZERO;
        }

        String method = asset.getAccountingDepreciationMethod();
        if (method == null) {
            method = DEPRECIATION_METHOD_LINEAR;
        }

        switch (method) {
            case DEPRECIATION_METHOD_LINEAR:
            case "straight_line":
                // Linear depreciation: (cost - salvage) * rate / 12
                BigDecimal depreciableAmount = asset.getAcquisitionCost()
                        .subtract(asset.getAccountingSalvageValue());
                BigDecimal annualAmount = depreciableAmount
                        .multiply(asset.getAccountingDepreciationRate())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                return annualAmount.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

            case DEPRECIATION_METHOD_DECLINING:
            case "declining_balance":
                // Declining balance: book_value * rate / 12
                BigDecimal monthlyRate = asset.getAccountingDepreciationRate()
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
                BigDecimal monthlyAmount = asset.getAccountingBookValue().multiply(monthlyRate);
                // Ensure we don't depreciate below salvage value
                BigDecimal maxDepreciation = asset.getAccountingBookValue()
                        .subtract(asset.getAccountingSalvageValue());
                return monthlyAmount.min(maxDepreciation);

            default:
                throw new IllegalStateException("Неподдържан метод на амортизация: " + method);
        }
    }

    /**
     * Calculate tax depreciation amount for a single month
     * Tax depreciation in Bulgaria is always straight-line
     */
    private BigDecimal calculateTaxDepreciation(FixedAssetEntity asset) {
        // If fully depreciated for tax purposes, return zero
        if (asset.getTaxBookValue().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Tax depreciation is always straight-line in Bulgaria
        BigDecimal annualAmount = asset.getAcquisitionCost()
                .multiply(asset.getTaxDepreciationRate())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal monthlyAmount = annualAmount.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        // Ensure we don't depreciate below zero
        return monthlyAmount.min(asset.getTaxBookValue());
    }

    /**
     * Validate that depreciation is calculated sequentially
     */
    private void validateSequentialPeriod(FixedAssetEntity asset, LocalDate period) {
        LocalDate startDate = asset.getPutIntoServiceDate();
        if (startDate == null) {
            startDate = asset.getAcquisitionDate();
        }

        LocalDate startPeriod = startDate.withDayOfMonth(1);

        // If this is the first period of service, no previous period to check
        if (!period.isAfter(startPeriod)) {
            return;
        }

        // Calculate previous period
        LocalDate previousPeriod = period.minusMonths(1);

        // Check if previous period exists (only if it's after the service start)
        if (!previousPeriod.isBefore(startPeriod)) {
            boolean previousExists = depreciationJournalRepository
                    .existsByFixedAssetIdAndPeriod(asset.getId(), previousPeriod);

            if (!previousExists) {
                throw new IllegalStateException(String.format(
                        "Не може да се изчисли амортизация за %d-%02d. Първо трябва да се изчисли за %d-%02d",
                        period.getYear(), period.getMonthValue(),
                        previousPeriod.getYear(), previousPeriod.getMonthValue()));
            }
        }
    }
}
