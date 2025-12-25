package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CheckCorrectionsInput;
import bg.spacbg.sp_ac_bg.model.dto.input.InventoryMovementFilter;
import bg.spacbg.sp_ac_bg.model.dto.input.QuantityTurnoverInput;
import bg.spacbg.sp_ac_bg.model.dto.inventory.AverageCostInfoDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.CorrectionNeededDto;
import bg.spacbg.sp_ac_bg.model.dto.inventory.QuantityTurnoverDto;
import bg.spacbg.sp_ac_bg.model.entity.InventoryBalanceEntity;
import bg.spacbg.sp_ac_bg.model.entity.InventoryMovementEntity;
import bg.spacbg.sp_ac_bg.service.InventoryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ========== Inventory Movement Queries ==========

    @QueryMapping
    public List<InventoryMovementEntity> inventoryMovements(@Argument InventoryMovementFilter filter) {
        return inventoryService.findMovementsByFilter(filter);
    }

    // ========== Inventory Balance Queries ==========

    @QueryMapping
    public List<InventoryBalanceEntity> inventoryBalances(@Argument Integer companyId) {
        return inventoryService.findBalancesByCompanyId(companyId);
    }

    @QueryMapping
    public InventoryBalanceEntity inventoryBalance(@Argument Integer companyId, @Argument Integer accountId) {
        return inventoryService.findBalance(companyId, accountId).orElse(null);
    }

    // ========== Количествена оборотна ведомост ==========

    @QueryMapping
    public List<QuantityTurnoverDto> getQuantityTurnover(@Argument QuantityTurnoverInput input) {
        return inventoryService.getQuantityTurnover(
                input.getCompanyId(),
                input.getFromDate(),
                input.getToDate()
        );
    }

    // ========== Средно претеглена цена (СПЦ) ==========

    @QueryMapping
    public AverageCostInfoDto getAverageCost(
            @Argument Integer companyId,
            @Argument Integer accountId,
            @Argument LocalDate asOfDate) {
        return inventoryService.getAverageCost(companyId, accountId, asOfDate);
    }

    // ========== Корекции на СПЦ ==========

    @QueryMapping
    public List<CorrectionNeededDto> checkRetroactiveCorrections(@Argument CheckCorrectionsInput input) {
        return inventoryService.checkRetroactiveCorrections(
                input.getCompanyId(),
                input.getAccountId(),
                input.getNewEntryDate()
        );
    }
}
