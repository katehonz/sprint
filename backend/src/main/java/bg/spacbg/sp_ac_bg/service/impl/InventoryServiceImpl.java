package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.InventoryMovementFilter;
import bg.spacbg.sp_ac_bg.model.dto.inventory.AverageCostInfoDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.CorrectionNeededDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.QuantityTurnoverDto;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final String DEBIT = "DEBIT";
    private static final String CREDIT = "CREDIT";
    private static final BigDecimal MIN_CORRECTION_THRESHOLD = new BigDecimal("0.01");

    private final InventoryMovementRepository movementRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final EntryLineRepository entryLineRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;

    public InventoryServiceImpl(
            InventoryMovementRepository movementRepository,
            InventoryBalanceRepository balanceRepository,
            EntryLineRepository entryLineRepository,
            AccountRepository accountRepository,
            CompanyRepository companyRepository) {
        this.movementRepository = movementRepository;
        this.balanceRepository = balanceRepository;
        this.entryLineRepository = entryLineRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
    }

    // ========== Inventory Movement Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementEntity> findMovementsByFilter(InventoryMovementFilter filter) {
        return movementRepository.findByFilter(
                filter.getCompanyId(),
                filter.getAccountId(),
                filter.getFromDate(),
                filter.getToDate(),
                filter.getMovementType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementEntity> findMovementsByJournalEntryId(Integer journalEntryId) {
        return movementRepository.findByJournalEntry_Id(journalEntryId);
    }

    // ========== Inventory Balance Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBalanceEntity> findBalancesByCompanyId(Integer companyId) {
        return balanceRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryBalanceEntity> findBalance(Integer companyId, Integer accountId) {
        return balanceRepository.findByCompanyIdAndAccountId(companyId, accountId);
    }

    // ========== Количествена оборотна ведомост ==========

    @Override
    @Transactional(readOnly = true)
    public List<QuantityTurnoverDto> getQuantityTurnover(Integer companyId, LocalDate fromDate, LocalDate toDate) {
        // Движения преди периода (за начално салдо)
        List<InventoryMovementEntity> openingMovements = movementRepository.findMovementsBeforeDate(companyId, fromDate);

        // Движения в периода
        List<InventoryMovementEntity> periodMovements = movementRepository.findMovementsInPeriod(companyId, fromDate, toDate);

        // Събираме уникални accountId
        Set<Integer> accountIds = new HashSet<>();
        openingMovements.forEach(m -> accountIds.add(m.getAccount().getId()));
        periodMovements.forEach(m -> accountIds.add(m.getAccount().getId()));

        // Зареждаме информация за сметките
        Map<Integer, AccountEntity> accountsMap = new HashMap<>();
        if (!accountIds.isEmpty()) {
            accountRepository.findAllById(accountIds).forEach(acc -> accountsMap.put(acc.getId(), acc));
        }

        // Изчисляваме количествена ведомост
        Map<Integer, QuantityTurnoverDto> turnoverMap = new HashMap<>();

        // Изчисляване на начални салда от движенията преди периода
        Map<Integer, BigDecimal> openingQuantities = new HashMap<>();
        Map<Integer, BigDecimal> openingAmounts = new HashMap<>();

        for (InventoryMovementEntity mov : openingMovements) {
            Integer accId = mov.getAccount().getId();
            // Използваме балансите след движението за последното движение за всяка сметка
            openingQuantities.put(accId, mov.getBalanceAfterQuantity());
            openingAmounts.put(accId, mov.getBalanceAfterAmount());
        }

        // Инициализираме всички сметки
        for (Integer accId : accountIds) {
            AccountEntity acc = accountsMap.get(accId);
            if (acc == null) continue;

            QuantityTurnoverDto dto = QuantityTurnoverDto.builder()
                    .accountId(accId)
                    .accountCode(acc.getCode())
                    .accountName(acc.getName())
                    .openingQuantity(openingQuantities.getOrDefault(accId, BigDecimal.ZERO))
                    .openingAmount(openingAmounts.getOrDefault(accId, BigDecimal.ZERO))
                    .receiptQuantity(BigDecimal.ZERO)
                    .receiptAmount(BigDecimal.ZERO)
                    .issueQuantity(BigDecimal.ZERO)
                    .issueAmount(BigDecimal.ZERO)
                    .closingQuantity(BigDecimal.ZERO)
                    .closingAmount(BigDecimal.ZERO)
                    .build();

            turnoverMap.put(accId, dto);
        }

        // Обработваме движенията в периода
        for (InventoryMovementEntity mov : periodMovements) {
            Integer accId = mov.getAccount().getId();
            QuantityTurnoverDto dto = turnoverMap.get(accId);
            if (dto == null) continue;

            if (DEBIT.equals(mov.getMovementType())) {
                // Приход
                dto.setReceiptQuantity(dto.getReceiptQuantity().add(mov.getQuantity()));
                dto.setReceiptAmount(dto.getReceiptAmount().add(mov.getTotalAmount()));
            } else {
                // Разход
                dto.setIssueQuantity(dto.getIssueQuantity().add(mov.getQuantity()));
                dto.setIssueAmount(dto.getIssueAmount().add(mov.getTotalAmount()));
            }
        }

        // Изчисляваме крайни салда
        for (QuantityTurnoverDto dto : turnoverMap.values()) {
            dto.setClosingQuantity(
                    dto.getOpeningQuantity()
                            .add(dto.getReceiptQuantity())
                            .subtract(dto.getIssueQuantity())
            );
            dto.setClosingAmount(
                    dto.getOpeningAmount()
                            .add(dto.getReceiptAmount())
                            .subtract(dto.getIssueAmount())
            );
        }

        // Сортираме по код на сметка
        List<QuantityTurnoverDto> result = new ArrayList<>(turnoverMap.values());
        result.sort(Comparator.comparing(QuantityTurnoverDto::getAccountCode));

        return result;
    }

    // ========== Средно претеглена цена (СПЦ) ==========

    @Override
    @Transactional(readOnly = true)
    public AverageCostInfoDto getAverageCost(Integer companyId, Integer accountId, LocalDate asOfDate) {
        if (asOfDate == null) {
            // Ако няма дата, връщаме текущия баланс
            Optional<InventoryBalanceEntity> balanceOpt = balanceRepository.findByCompanyIdAndAccountId(companyId, accountId);
            if (balanceOpt.isPresent()) {
                InventoryBalanceEntity balance = balanceOpt.get();
                return AverageCostInfoDto.builder()
                        .accountId(accountId)
                        .currentQuantity(balance.getCurrentQuantity())
                        .currentAmount(balance.getCurrentAmount())
                        .averageCost(balance.getCurrentAverageCost())
                        .build();
            }
            return AverageCostInfoDto.builder()
                    .accountId(accountId)
                    .currentQuantity(BigDecimal.ZERO)
                    .currentAmount(BigDecimal.ZERO)
                    .averageCost(BigDecimal.ZERO)
                    .build();
        }

        // Изчисляваме СПЦ към определена дата
        return calculateAverageCostAtDate(companyId, accountId, asOfDate);
    }

    private AverageCostInfoDto calculateAverageCostAtDate(Integer companyId, Integer accountId, LocalDate asOfDate) {
        List<InventoryMovementEntity> movements = movementRepository.findMovementsUpToDate(companyId, accountId, asOfDate);

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        for (InventoryMovementEntity mov : movements) {
            if (DEBIT.equals(mov.getMovementType())) {
                quantity = quantity.add(mov.getQuantity());
                amount = amount.add(mov.getTotalAmount());
            } else {
                // При изписване използваме текущата СПЦ
                BigDecimal avgCost = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? amount.divide(quantity, 6, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal issueValue = avgCost.multiply(mov.getQuantity());
                quantity = quantity.subtract(mov.getQuantity());
                amount = amount.subtract(issueValue);
            }
        }

        BigDecimal averageCost = quantity.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(quantity, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return AverageCostInfoDto.builder()
                .accountId(accountId)
                .currentQuantity(quantity)
                .currentAmount(amount)
                .averageCost(averageCost)
                .build();
    }

    // ========== Корекции на СПЦ ==========

    @Override
    @Transactional(readOnly = true)
    public List<CorrectionNeededDto> checkRetroactiveCorrections(Integer companyId, Integer accountId, LocalDate newEntryDate) {
        List<CorrectionNeededDto> corrections = new ArrayList<>();

        // Намираме всички CREDIT движения след новата дата
        List<InventoryMovementEntity> affectedMovements =
                movementRepository.findCreditMovementsAfterDate(companyId, accountId, newEntryDate);

        // Зареждаме информация за материалната сметка
        AccountEntity materialAccount = accountRepository.findById(accountId).orElse(null);

        for (InventoryMovementEntity movement : affectedMovements) {
            // Намираме дебитната сметка от същия журнален запис
            List<EntryLineEntity> entryLines = entryLineRepository.findByJournalEntry_Id(movement.getJournalEntry().getId());

            // Търсим дебитната сметка (разходна) със същата сума
            EntryLineEntity debitLine = entryLines.stream()
                    .filter(line -> !line.getAccount().getId().equals(accountId)
                            && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                            && line.getDebitAmount().compareTo(movement.getTotalAmount()) == 0)
                    .findFirst()
                    .orElse(null);

            if (debitLine == null) continue;

            AccountEntity expenseAccount = debitLine.getAccount();

            // Изчисляваме новата СПЦ на датата на движението
            AverageCostInfoDto newCostInfo = calculateAverageCostAtDate(companyId, accountId, movement.getMovementDate());

            BigDecimal oldAvgCost = movement.getAverageCostAtTime();
            BigDecimal newAvgCost = newCostInfo.getAverageCost();
            BigDecimal qty = movement.getQuantity();

            BigDecimal correctionAmount = newAvgCost.subtract(oldAvgCost).multiply(qty);

            // Добавяме корекция само ако разликата е значителна
            if (correctionAmount.abs().compareTo(MIN_CORRECTION_THRESHOLD) > 0) {
                String description = String.format("Корекция СПЦ за %.2f бр от %.4f на %.4f лв",
                        qty.doubleValue(), oldAvgCost.doubleValue(), newAvgCost.doubleValue());

                corrections.add(CorrectionNeededDto.builder()
                        .movementId(movement.getId())
                        .movementDate(movement.getMovementDate())
                        .materialAccountId(accountId)
                        .materialAccountCode(materialAccount != null ? materialAccount.getCode() : "N/A")
                        .materialAccountName(materialAccount != null ? materialAccount.getName() : "N/A")
                        .expenseAccountId(expenseAccount.getId())
                        .expenseAccountCode(expenseAccount.getCode())
                        .expenseAccountName(expenseAccount.getName())
                        .quantity(qty)
                        .oldAverageCost(oldAvgCost)
                        .newAverageCost(newAvgCost)
                        .correctionAmount(correctionAmount)
                        .description(description)
                        .build());
            }
        }

        return corrections;
    }

    // ========== Обработка на движения ==========

    @Override
    public Optional<InventoryMovementEntity> processEntryLine(Integer entryLineId) {
        // Проверяваме дали вече има движение за този entry line
        Optional<InventoryMovementEntity> existing = movementRepository.findByEntryLine_Id(entryLineId);
        if (existing.isPresent()) {
            return existing;
        }

        EntryLineEntity entryLine = entryLineRepository.findById(entryLineId).orElse(null);
        if (entryLine == null) {
            return Optional.empty();
        }

        // Проверяваме дали има количество
        if (entryLine.getQuantity() == null || entryLine.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        // Проверяваме дали сметката поддържа количества
        AccountEntity account = entryLine.getAccount();
        if (!account.isSupportsQuantities()) {
            return Optional.empty();
        }

        JournalEntryEntity journalEntry = entryLine.getJournalEntry();
        CompanyEntity company = journalEntry.getCompany();

        BigDecimal quantity = entryLine.getQuantity();
        String movementType = entryLine.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 ? DEBIT : CREDIT;
        BigDecimal amount = entryLine.getDebitAmount().max(entryLine.getCreditAmount());
        BigDecimal unitPrice = quantity.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(quantity, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Получаваме или създаваме баланс
        InventoryBalanceEntity balance = getOrCreateBalance(company, account);

        // Изчисляваме нови стойности
        BigDecimal newQuantity;
        BigDecimal newAmount;
        BigDecimal newAvgCost;

        if (DEBIT.equals(movementType)) {
            // Приход - рекалкулираме СПЦ
            newQuantity = balance.getCurrentQuantity().add(quantity);
            newAmount = balance.getCurrentAmount().add(amount);
            newAvgCost = newQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? newAmount.divide(newQuantity, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } else {
            // Разход - използваме текущата СПЦ
            BigDecimal issueValue = balance.getCurrentAverageCost().multiply(quantity);
            newQuantity = balance.getCurrentQuantity().subtract(quantity);
            newAmount = balance.getCurrentAmount().subtract(issueValue);
            newAvgCost = balance.getCurrentAverageCost(); // СПЦ не се променя при изписване
        }

        // Създаваме движение
        InventoryMovementEntity movement = new InventoryMovementEntity();
        movement.setCompany(company);
        movement.setAccount(account);
        movement.setEntryLine(entryLine);
        movement.setJournalEntry(journalEntry);
        movement.setMovementDate(journalEntry.getAccountingDate());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setUnitPrice(unitPrice);
        movement.setTotalAmount(amount);
        movement.setUnitOfMeasure(entryLine.getUnitOfMeasureCode());
        movement.setDescription(entryLine.getDescription());
        movement.setBalanceAfterQuantity(newQuantity);
        movement.setBalanceAfterAmount(newAmount);
        movement.setAverageCostAtTime(newAvgCost);

        InventoryMovementEntity savedMovement = movementRepository.save(movement);

        // Обновяваме баланса
        balance.setCurrentQuantity(newQuantity);
        balance.setCurrentAmount(newAmount);
        balance.setCurrentAverageCost(newAvgCost);
        balance.setLastMovementDate(journalEntry.getAccountingDate());
        balance.setLastMovement(savedMovement);
        balanceRepository.save(balance);

        return Optional.of(savedMovement);
    }

    @Override
    public void deleteMovementsByJournalEntryId(Integer journalEntryId) {
        // Намираме движенията за изтриване
        List<InventoryMovementEntity> movements = movementRepository.findByJournalEntry_Id(journalEntryId);

        if (movements.isEmpty()) {
            return;
        }

        // За всяко движение трябва да преизчислим баланса
        for (InventoryMovementEntity movement : movements) {
            Integer companyId = movement.getCompany().getId();
            Integer accountId = movement.getAccount().getId();

            // Изтриваме движението
            movementRepository.delete(movement);

            // Преизчисляваме баланса от всички останали движения
            recalculateBalance(companyId, accountId);
        }
    }

    private InventoryBalanceEntity getOrCreateBalance(CompanyEntity company, AccountEntity account) {
        return balanceRepository.findByCompanyIdAndAccountId(company.getId(), account.getId())
                .orElseGet(() -> {
                    InventoryBalanceEntity newBalance = new InventoryBalanceEntity();
                    newBalance.setCompany(company);
                    newBalance.setAccount(account);
                    newBalance.setCurrentQuantity(BigDecimal.ZERO);
                    newBalance.setCurrentAmount(BigDecimal.ZERO);
                    newBalance.setCurrentAverageCost(BigDecimal.ZERO);
                    return balanceRepository.save(newBalance);
                });
    }

    private void recalculateBalance(Integer companyId, Integer accountId) {
        // Намираме всички движения за тази сметка
        List<InventoryMovementEntity> movements = movementRepository.findMovementsUpToDate(
                companyId, accountId, LocalDate.of(9999, 12, 31));

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        for (InventoryMovementEntity mov : movements) {
            if (DEBIT.equals(mov.getMovementType())) {
                quantity = quantity.add(mov.getQuantity());
                amount = amount.add(mov.getTotalAmount());
            } else {
                BigDecimal avgCost = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? amount.divide(quantity, 6, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal issueValue = avgCost.multiply(mov.getQuantity());
                quantity = quantity.subtract(mov.getQuantity());
                amount = amount.subtract(issueValue);
            }
        }

        BigDecimal avgCost = quantity.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(quantity, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Обновяваме баланса
        Optional<InventoryBalanceEntity> balanceOpt = balanceRepository.findByCompanyIdAndAccountId(companyId, accountId);
        if (balanceOpt.isPresent()) {
            InventoryBalanceEntity balance = balanceOpt.get();
            balance.setCurrentQuantity(quantity);
            balance.setCurrentAmount(amount);
            balance.setCurrentAverageCost(avgCost);

            if (!movements.isEmpty()) {
                InventoryMovementEntity lastMovement = movements.get(movements.size() - 1);
                balance.setLastMovementDate(lastMovement.getMovementDate());
                balance.setLastMovement(lastMovement);
            } else {
                balance.setLastMovementDate(null);
                balance.setLastMovement(null);
            }

            balanceRepository.save(balance);
        }
    }
}
