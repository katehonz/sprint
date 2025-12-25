package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.CalculatedPeriodDto;
import bg.spacbg.sp_ac_bg.model.dto.DepreciationCalculationResultDto;
import bg.spacbg.sp_ac_bg.model.dto.DepreciationJournalDto;
import bg.spacbg.sp_ac_bg.model.dto.DepreciationPostResultDto;
import bg.spacbg.sp_ac_bg.model.entity.DepreciationJournalEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;
import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;
import bg.spacbg.sp_ac_bg.service.DepreciationService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DepreciationController {

    private final DepreciationService depreciationService;

    public DepreciationController(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    // ========== Queries ==========

    @QueryMapping
    public List<DepreciationJournalDto> depreciationJournal(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        List<DepreciationJournalEntity> entries = depreciationService.getDepreciationJournal(companyId, year, month);
        return entries.stream()
                .map(DepreciationJournalDto::fromEntity)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<CalculatedPeriodDto> calculatedPeriods(@Argument Integer companyId) {
        return depreciationService.getCalculatedPeriods(companyId).stream()
                .map(CalculatedPeriodDto::fromRecord)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<FixedAssetEntity> assetsNeedingDepreciation(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        LocalDate period = LocalDate.of(year, month, 1);
        return depreciationService.getAssetsNeedingDepreciation(companyId, period);
    }

    // ========== Mutations ==========

    @MutationMapping
    public DepreciationCalculationResultDto calculateMonthlyDepreciation(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        LocalDate period = LocalDate.of(year, month, 1);
        DepreciationService.DepreciationCalculationResult result =
                depreciationService.calculateBulkDepreciation(companyId, period);
        return DepreciationCalculationResultDto.fromResult(result);
    }

    @MutationMapping
    public DepreciationPostResultDto postDepreciation(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        LocalDate period = LocalDate.of(year, month, 1);
        // TODO: Get userId from security context
        Integer userId = 1;
        JournalEntryEntity journalEntry = depreciationService.postDepreciation(companyId, period, userId);

        // Count posted assets
        List<DepreciationJournalEntity> posted = depreciationService.getDepreciationJournal(companyId, year, month);
        int assetsCount = (int) posted.stream().filter(DepreciationJournalEntity::isPosted).count();

        return new DepreciationPostResultDto(
                journalEntry.getId(),
                journalEntry.getTotalAmount(),
                assetsCount
        );
    }
}
