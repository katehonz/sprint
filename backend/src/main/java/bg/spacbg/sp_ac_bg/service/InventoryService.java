package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.InventoryMovementFilter;
import bg.spacbg.sp_ac_bg.model.dto.inventory.AverageCostInfoDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.CorrectionNeededDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.QuantityTurnoverDto;
import bg.spacbg.sp_ac_bg.model.entity.InventoryBalanceEntity;
import bg.spacbg.sp_ac_bg.model.entity.InventoryMovementEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryService {
    // Inventory Movement operations
    List<InventoryMovementEntity> findMovementsByFilter(InventoryMovementFilter filter);
    List<InventoryMovementEntity> findMovementsByJournalEntryId(Integer journalEntryId);

    // Inventory Balance operations
    List<InventoryBalanceEntity> findBalancesByCompanyId(Integer companyId);
    Optional<InventoryBalanceEntity> findBalance(Integer companyId, Integer accountId);

    // ========== Количествена оборотна ведомост ==========

    /**
     * Генерира количествена оборотна ведомост за период
     */
    List<QuantityTurnoverDto> getQuantityTurnover(Integer companyId, LocalDate fromDate, LocalDate toDate);

    // ========== Средно претеглена цена (СПЦ) ==========

    /**
     * Изчислява текущата СПЦ за материална сметка
     */
    AverageCostInfoDto getAverageCost(Integer companyId, Integer accountId, LocalDate asOfDate);

    // ========== Корекции на СПЦ ==========

    /**
     * Проверява за необходими корекции при добавяне на ретроактивен запис
     */
    List<CorrectionNeededDto> checkRetroactiveCorrections(Integer companyId, Integer accountId, LocalDate newEntryDate);

    // ========== Обработка на движения ==========

    /**
     * Обработва entry line и създава inventory movement
     * Извиква се автоматично при осчетоводяване на журнален запис
     */
    Optional<InventoryMovementEntity> processEntryLine(Integer entryLineId);

    /**
     * Изтрива движенията за journal entry (при отосчетоводяване)
     */
    void deleteMovementsByJournalEntryId(Integer journalEntryId);
}
