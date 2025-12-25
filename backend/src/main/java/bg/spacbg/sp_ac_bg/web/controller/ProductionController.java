package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchEntity;
import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchStageEntity;
import bg.spacbg.sp_ac_bg.model.entity.TechnologyCardEntity;
import bg.spacbg.sp_ac_bg.service.ProductionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    // ========== Technology Card Queries ==========

    @QueryMapping
    public List<TechnologyCardEntity> technologyCards(@Argument Integer companyId) {
        return productionService.findTechnologyCardsByCompanyId(companyId);
    }

    @QueryMapping
    public List<TechnologyCardEntity> activeTechnologyCards(@Argument Integer companyId) {
        return productionService.findActiveTechnologyCardsByCompanyId(companyId);
    }

    @QueryMapping
    public TechnologyCardEntity technologyCard(@Argument Integer id) {
        return productionService.findTechnologyCardByIdWithStages(id).orElse(null);
    }

    // ========== Production Batch Queries ==========

    @QueryMapping
    public List<ProductionBatchEntity> productionBatches(@Argument Integer companyId) {
        return productionService.findProductionBatchesByCompanyId(companyId);
    }

    @QueryMapping
    public List<ProductionBatchEntity> productionBatchesByFilter(@Argument ProductionBatchFilterInput filter) {
        return productionService.findProductionBatchesByFilter(filter);
    }

    @QueryMapping
    public ProductionBatchEntity productionBatch(@Argument Integer id) {
        return productionService.findProductionBatchByIdWithDetails(id).orElse(null);
    }

    @QueryMapping
    public List<ProductionBatchStageEntity> productionBatchStages(@Argument Integer productionBatchId) {
        return productionService.findStagesByProductionBatchId(productionBatchId);
    }

    // ========== Technology Card Mutations ==========

    @MutationMapping
    public TechnologyCardEntity createTechnologyCard(@Argument CreateTechnologyCardInput input) {
        return productionService.createTechnologyCard(input);
    }

    @MutationMapping
    public TechnologyCardEntity updateTechnologyCard(@Argument UpdateTechnologyCardInput input) {
        return productionService.updateTechnologyCard(input);
    }

    @MutationMapping
    public boolean deleteTechnologyCard(@Argument Integer id) {
        return productionService.deleteTechnologyCard(id);
    }

    // ========== Production Batch Mutations ==========

    @MutationMapping
    public ProductionBatchEntity createProductionBatch(@Argument CreateProductionBatchInput input) {
        return productionService.createProductionBatch(input);
    }

    @MutationMapping
    public ProductionBatchEntity updateProductionBatch(@Argument UpdateProductionBatchInput input) {
        return productionService.updateProductionBatch(input);
    }

    @MutationMapping
    public ProductionBatchEntity startProductionBatch(@Argument Integer id) {
        return productionService.startProductionBatch(id);
    }

    @MutationMapping
    public ProductionBatchEntity completeProductionBatch(@Argument Integer id) {
        return productionService.completeProductionBatch(id);
    }

    @MutationMapping
    public ProductionBatchEntity cancelProductionBatch(@Argument Integer id) {
        return productionService.cancelProductionBatch(id);
    }

    @MutationMapping
    public boolean deleteProductionBatch(@Argument Integer id) {
        return productionService.deleteProductionBatch(id);
    }

    // ========== Production Batch Stage Mutations ==========

    @MutationMapping
    public ProductionBatchStageEntity completeProductionStage(@Argument CompleteProductionStageInput input) {
        return productionService.completeProductionStage(input);
    }

    @MutationMapping
    public ProductionBatchStageEntity cancelProductionStage(@Argument Integer id) {
        return productionService.cancelProductionStage(id);
    }
}
