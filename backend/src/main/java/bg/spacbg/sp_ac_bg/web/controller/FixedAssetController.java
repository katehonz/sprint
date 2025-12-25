package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetCategoryEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;
import bg.spacbg.sp_ac_bg.service.FixedAssetService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    public FixedAssetController(FixedAssetService fixedAssetService) {
        this.fixedAssetService = fixedAssetService;
    }

    // ========== Fixed Asset Queries ==========

    @QueryMapping
    public List<FixedAssetEntity> fixedAssets(@Argument Integer companyId) {
        return fixedAssetService.findAssetsByCompanyId(companyId);
    }

    @QueryMapping
    public FixedAssetEntity fixedAsset(@Argument Integer id) {
        return fixedAssetService.findAssetById(id).orElse(null);
    }

    @QueryMapping
    public List<FixedAssetEntity> fixedAssetsByCategory(@Argument Integer categoryId) {
        return fixedAssetService.findAssetsByCategoryId(categoryId);
    }

    // ========== Fixed Asset Category Queries ==========

    @QueryMapping
    public List<FixedAssetCategoryEntity> fixedAssetCategories(@Argument Integer companyId) {
        // Note: Categories are global, companyId is for future filtering
        return fixedAssetService.findAllCategories();
    }

    @QueryMapping
    public FixedAssetCategoryEntity fixedAssetCategory(@Argument Integer id) {
        return fixedAssetService.findCategoryById(id).orElse(null);
    }

    // ========== Fixed Asset Mutations ==========

    @MutationMapping
    public FixedAssetEntity createFixedAsset(@Argument CreateFixedAssetInput input) {
        return fixedAssetService.createAsset(input);
    }

    @MutationMapping
    public FixedAssetEntity updateFixedAsset(@Argument Integer id, @Argument UpdateFixedAssetInput input) {
        return fixedAssetService.updateAsset(id, input);
    }

    @MutationMapping
    public boolean deleteFixedAsset(@Argument Integer id) {
        return fixedAssetService.deleteAsset(id);
    }

    @MutationMapping
    public FixedAssetEntity disposeFixedAsset(
            @Argument Integer id,
            @Argument LocalDate disposalDate,
            @Argument BigDecimal disposalAmount) {
        return fixedAssetService.disposeAsset(id, disposalDate, disposalAmount);
    }

    @MutationMapping
    public List<FixedAssetEntity> calculateDepreciation(
            @Argument Integer companyId,
            @Argument LocalDate toDate) {
        return fixedAssetService.calculateDepreciation(companyId, toDate);
    }

    // ========== Fixed Asset Category Mutations ==========

    @MutationMapping
    public FixedAssetCategoryEntity createFixedAssetCategory(@Argument CreateFixedAssetCategoryInput input) {
        return fixedAssetService.createCategory(input);
    }

    @MutationMapping
    public FixedAssetCategoryEntity updateFixedAssetCategory(
            @Argument Integer id,
            @Argument UpdateFixedAssetCategoryInput input) {
        return fixedAssetService.updateCategory(id, input);
    }
}
