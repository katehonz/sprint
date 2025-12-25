package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchEntity;
import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchStageEntity;
import bg.spacbg.sp_ac_bg.model.entity.TechnologyCardEntity;

import java.util.List;
import java.util.Optional;

public interface ProductionService {

    // ========== Technology Card Operations ==========

    List<TechnologyCardEntity> findTechnologyCardsByCompanyId(Integer companyId);

    List<TechnologyCardEntity> findActiveTechnologyCardsByCompanyId(Integer companyId);

    Optional<TechnologyCardEntity> findTechnologyCardById(Integer id);

    Optional<TechnologyCardEntity> findTechnologyCardByIdWithStages(Integer id);

    TechnologyCardEntity createTechnologyCard(CreateTechnologyCardInput input);

    TechnologyCardEntity updateTechnologyCard(UpdateTechnologyCardInput input);

    boolean deleteTechnologyCard(Integer id);

    // ========== Production Batch Operations ==========

    List<ProductionBatchEntity> findProductionBatchesByCompanyId(Integer companyId);

    List<ProductionBatchEntity> findProductionBatchesByFilter(ProductionBatchFilterInput filter);

    Optional<ProductionBatchEntity> findProductionBatchById(Integer id);

    Optional<ProductionBatchEntity> findProductionBatchByIdWithDetails(Integer id);

    ProductionBatchEntity createProductionBatch(CreateProductionBatchInput input);

    ProductionBatchEntity updateProductionBatch(UpdateProductionBatchInput input);

    ProductionBatchEntity startProductionBatch(Integer id);

    ProductionBatchEntity completeProductionBatch(Integer id);

    ProductionBatchEntity cancelProductionBatch(Integer id);

    boolean deleteProductionBatch(Integer id);

    // ========== Production Batch Stage Operations ==========

    List<ProductionBatchStageEntity> findStagesByProductionBatchId(Integer productionBatchId);

    ProductionBatchStageEntity completeProductionStage(CompleteProductionStageInput input);

    ProductionBatchStageEntity cancelProductionStage(Integer stageId);

    // ========== Statistics ==========

    long countTechnologyCardsByCompanyId(Integer companyId);

    long countProductionBatchesByCompanyId(Integer companyId);

    long countActiveProductionBatchesByCompanyId(Integer companyId);
}
