package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.entity.DepreciationJournalEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;
import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing fixed asset depreciation calculations
 * according to Bulgarian accounting and tax standards (ЗКПО)
 */
public interface DepreciationService {

    /**
     * Calculate monthly depreciation for a single asset
     * @param assetId Fixed asset ID
     * @param period Period (first day of month)
     * @return The depreciation journal entry
     */
    DepreciationJournalEntity calculateMonthlyDepreciation(Integer assetId, LocalDate period);

    /**
     * Calculate depreciation for all active assets in a company for a specific period
     * @param companyId Company ID
     * @param period Period (first day of month)
     * @return Result with calculated depreciations and any errors
     */
    DepreciationCalculationResult calculateBulkDepreciation(Integer companyId, LocalDate period);

    /**
     * Get depreciation journal entries for a company and period
     * @param companyId Company ID
     * @param year Year
     * @param month Month (optional - if null, returns all months in year)
     * @return List of depreciation journal entries
     */
    List<DepreciationJournalEntity> getDepreciationJournal(Integer companyId, Integer year, Integer month);

    /**
     * Get assets that need depreciation for a period
     * @param companyId Company ID
     * @param period Period
     * @return List of assets needing depreciation
     */
    List<FixedAssetEntity> getAssetsNeedingDepreciation(Integer companyId, LocalDate period);

    /**
     * Get calculated periods for a company
     * @param companyId Company ID
     * @return List of periods (first day of month)
     */
    List<CalculatedPeriod> getCalculatedPeriods(Integer companyId);

    /**
     * Post depreciation to accounting (create journal entry)
     * @param companyId Company ID
     * @param period Period
     * @param userId User performing the post
     * @return The created journal entry
     */
    JournalEntryEntity postDepreciation(Integer companyId, LocalDate period, Integer userId);

    /**
     * Depreciation calculation result
     */
    record DepreciationCalculationResult(
            List<DepreciationJournalEntity> calculated,
            List<DepreciationError> errors,
            java.math.BigDecimal totalAccountingAmount,
            java.math.BigDecimal totalTaxAmount
    ) {}

    /**
     * Depreciation error
     */
    record DepreciationError(
            Integer fixedAssetId,
            String assetName,
            String errorMessage
    ) {}

    /**
     * Calculated period info
     */
    record CalculatedPeriod(
            Integer year,
            Integer month,
            String periodDisplay,
            boolean isPosted,
            java.math.BigDecimal totalAccountingAmount,
            java.math.BigDecimal totalTaxAmount,
            Integer assetsCount
    ) {}
}
