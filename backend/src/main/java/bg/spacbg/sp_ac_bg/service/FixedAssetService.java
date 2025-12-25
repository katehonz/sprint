package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetCategoryEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FixedAssetService {
    // Fixed Asset operations
    List<FixedAssetEntity> findAssetsByCompanyId(Integer companyId);
    List<FixedAssetEntity> findAssetsByCategoryId(Integer categoryId);
    Optional<FixedAssetEntity> findAssetById(Integer id);
    FixedAssetEntity createAsset(CreateFixedAssetInput input);
    FixedAssetEntity updateAsset(Integer id, UpdateFixedAssetInput input);
    boolean deleteAsset(Integer id);
    FixedAssetEntity disposeAsset(Integer id, LocalDate disposalDate, BigDecimal disposalAmount);
    List<FixedAssetEntity> calculateDepreciation(Integer companyId, LocalDate toDate);

    // Fixed Asset Category operations
    List<FixedAssetCategoryEntity> findAllCategories();
    Optional<FixedAssetCategoryEntity> findCategoryById(Integer id);
    FixedAssetCategoryEntity createCategory(CreateFixedAssetCategoryInput input);
    FixedAssetCategoryEntity updateCategory(Integer id, UpdateFixedAssetCategoryInput input);
}
