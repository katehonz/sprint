package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStageStatus;
import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStatus;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import bg.spacbg.sp_ac_bg.service.ProductionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductionServiceImpl implements ProductionService {

    private final TechnologyCardRepository technologyCardRepository;
    private final TechnologyCardStageRepository technologyCardStageRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionBatchStageRepository productionBatchStageRepository;
    private final CompanyService companyService;
    private final AccountRepository accountRepository;

    public ProductionServiceImpl(
            TechnologyCardRepository technologyCardRepository,
            TechnologyCardStageRepository technologyCardStageRepository,
            ProductionBatchRepository productionBatchRepository,
            ProductionBatchStageRepository productionBatchStageRepository,
            CompanyService companyService,
            AccountRepository accountRepository) {
        this.technologyCardRepository = technologyCardRepository;
        this.technologyCardStageRepository = technologyCardStageRepository;
        this.productionBatchRepository = productionBatchRepository;
        this.productionBatchStageRepository = productionBatchStageRepository;
        this.companyService = companyService;
        this.accountRepository = accountRepository;
    }

    // ========== Technology Card Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<TechnologyCardEntity> findTechnologyCardsByCompanyId(Integer companyId) {
        return technologyCardRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechnologyCardEntity> findActiveTechnologyCardsByCompanyId(Integer companyId) {
        return technologyCardRepository.findActiveByCompanyIdWithStages(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TechnologyCardEntity> findTechnologyCardById(Integer id) {
        return technologyCardRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TechnologyCardEntity> findTechnologyCardByIdWithStages(Integer id) {
        return technologyCardRepository.findByIdWithStages(id);
    }

    @Override
    public TechnologyCardEntity createTechnologyCard(CreateTechnologyCardInput input) {
        CompanyEntity company = companyService.findById(input.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + input.getCompanyId()));

        AccountEntity outputAccount = accountRepository.findById(input.getOutputAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Output account not found: " + input.getOutputAccountId()));

        if (technologyCardRepository.existsByCompanyIdAndCode(input.getCompanyId(), input.getCode())) {
            throw new IllegalArgumentException("Technology card with code " + input.getCode() + " already exists");
        }

        TechnologyCardEntity card = new TechnologyCardEntity();
        card.setCompany(company);
        card.setCode(input.getCode());
        card.setName(input.getName());
        card.setDescription(input.getDescription());
        card.setOutputAccount(outputAccount);
        card.setOutputQuantity(input.getOutputQuantity() != null ? input.getOutputQuantity() : BigDecimal.ONE);
        card.setOutputUnit(input.getOutputUnit());
        card.setActive(true);

        card = technologyCardRepository.save(card);

        // Create stages
        if (input.getStages() != null && !input.getStages().isEmpty()) {
            List<TechnologyCardStageEntity> stages = new ArrayList<>();
            for (TechnologyCardStageInput stageInput : input.getStages()) {
                TechnologyCardStageEntity stage = createStageFromInput(card, stageInput);
                stages.add(stage);
            }
            technologyCardStageRepository.saveAll(stages);
            card.setStages(stages);
        }

        return card;
    }

    @Override
    public TechnologyCardEntity updateTechnologyCard(UpdateTechnologyCardInput input) {
        TechnologyCardEntity card = technologyCardRepository.findById(input.getId())
                .orElseThrow(() -> new EntityNotFoundException("Technology card not found: " + input.getId()));

        if (input.getCode() != null && !input.getCode().equals(card.getCode())) {
            if (technologyCardRepository.existsByCompanyIdAndCode(card.getCompany().getId(), input.getCode())) {
                throw new IllegalArgumentException("Technology card with code " + input.getCode() + " already exists");
            }
            card.setCode(input.getCode());
        }

        if (input.getName() != null) {
            card.setName(input.getName());
        }
        if (input.getDescription() != null) {
            card.setDescription(input.getDescription());
        }
        if (input.getOutputAccountId() != null) {
            AccountEntity outputAccount = accountRepository.findById(input.getOutputAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Output account not found: " + input.getOutputAccountId()));
            card.setOutputAccount(outputAccount);
        }
        if (input.getOutputQuantity() != null) {
            card.setOutputQuantity(input.getOutputQuantity());
        }
        if (input.getOutputUnit() != null) {
            card.setOutputUnit(input.getOutputUnit());
        }
        if (input.getIsActive() != null) {
            card.setActive(input.getIsActive());
        }

        // Update stages if provided
        if (input.getStages() != null) {
            // Delete existing stages
            technologyCardStageRepository.deleteByTechnologyCardId(card.getId());

            // Create new stages
            List<TechnologyCardStageEntity> stages = new ArrayList<>();
            for (TechnologyCardStageInput stageInput : input.getStages()) {
                TechnologyCardStageEntity stage = createStageFromInput(card, stageInput);
                stages.add(stage);
            }
            technologyCardStageRepository.saveAll(stages);
            card.setStages(stages);
        }

        return technologyCardRepository.save(card);
    }

    @Override
    public boolean deleteTechnologyCard(Integer id) {
        if (!technologyCardRepository.existsById(id)) {
            return false;
        }

        // Check if there are production batches using this card
        List<ProductionBatchEntity> batches = productionBatchRepository.findByTechnologyCardId(id);
        if (!batches.isEmpty()) {
            throw new IllegalStateException("Cannot delete technology card with existing production batches");
        }

        technologyCardRepository.deleteById(id);
        return true;
    }

    // ========== Production Batch Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<ProductionBatchEntity> findProductionBatchesByCompanyId(Integer companyId) {
        return productionBatchRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionBatchEntity> findProductionBatchesByFilter(ProductionBatchFilterInput filter) {
        return productionBatchRepository.findByFilters(
                filter.getCompanyId(),
                filter.getStatus(),
                filter.getTechnologyCardId(),
                filter.getStartDate(),
                filter.getEndDate()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductionBatchEntity> findProductionBatchById(Integer id) {
        return productionBatchRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductionBatchEntity> findProductionBatchByIdWithDetails(Integer id) {
        return productionBatchRepository.findByIdWithDetails(id);
    }

    @Override
    public ProductionBatchEntity createProductionBatch(CreateProductionBatchInput input) {
        CompanyEntity company = companyService.findById(input.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + input.getCompanyId()));

        TechnologyCardEntity technologyCard = technologyCardRepository.findByIdWithStages(input.getTechnologyCardId())
                .orElseThrow(() -> new EntityNotFoundException("Technology card not found: " + input.getTechnologyCardId()));

        if (productionBatchRepository.existsByCompanyIdAndBatchNumber(input.getCompanyId(), input.getBatchNumber())) {
            throw new IllegalArgumentException("Production batch with number " + input.getBatchNumber() + " already exists");
        }

        ProductionBatchEntity batch = new ProductionBatchEntity();
        batch.setCompany(company);
        batch.setTechnologyCard(technologyCard);
        batch.setBatchNumber(input.getBatchNumber());
        batch.setPlannedQuantity(input.getPlannedQuantity());
        batch.setStatus(ProductionBatchStatus.PLANNED);
        batch.setNotes(input.getNotes());

        batch = productionBatchRepository.save(batch);

        // Create stages based on technology card stages
        if (technologyCard.getStages() != null && !technologyCard.getStages().isEmpty()) {
            List<ProductionBatchStageEntity> batchStages = new ArrayList<>();
            for (TechnologyCardStageEntity tcStage : technologyCard.getStages()) {
                ProductionBatchStageEntity batchStage = new ProductionBatchStageEntity();
                batchStage.setProductionBatch(batch);
                batchStage.setTechnologyCardStage(tcStage);
                batchStage.setPlannedQuantity(tcStage.getInputQuantity().multiply(input.getPlannedQuantity()));
                batchStage.setStatus(ProductionBatchStageStatus.PENDING);
                batchStages.add(batchStage);
            }
            productionBatchStageRepository.saveAll(batchStages);
            batch.setStages(batchStages);
        }

        return batch;
    }

    @Override
    public ProductionBatchEntity updateProductionBatch(UpdateProductionBatchInput input) {
        ProductionBatchEntity batch = productionBatchRepository.findById(input.getId())
                .orElseThrow(() -> new EntityNotFoundException("Production batch not found: " + input.getId()));

        if (input.getBatchNumber() != null && !input.getBatchNumber().equals(batch.getBatchNumber())) {
            if (productionBatchRepository.existsByCompanyIdAndBatchNumber(batch.getCompany().getId(), input.getBatchNumber())) {
                throw new IllegalArgumentException("Production batch with number " + input.getBatchNumber() + " already exists");
            }
            batch.setBatchNumber(input.getBatchNumber());
        }

        if (input.getPlannedQuantity() != null) {
            batch.setPlannedQuantity(input.getPlannedQuantity());
        }
        if (input.getActualQuantity() != null) {
            batch.setActualQuantity(input.getActualQuantity());
        }
        if (input.getStatus() != null) {
            batch.setStatus(input.getStatus());
        }
        if (input.getNotes() != null) {
            batch.setNotes(input.getNotes());
        }

        return productionBatchRepository.save(batch);
    }

    @Override
    public ProductionBatchEntity startProductionBatch(Integer id) {
        ProductionBatchEntity batch = productionBatchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Production batch not found: " + id));

        if (batch.getStatus() != ProductionBatchStatus.PLANNED) {
            throw new IllegalStateException("Only planned batches can be started");
        }

        batch.setStatus(ProductionBatchStatus.IN_PROGRESS);
        batch.setStartedAt(OffsetDateTime.now());

        return productionBatchRepository.save(batch);
    }

    @Override
    public ProductionBatchEntity completeProductionBatch(Integer id) {
        ProductionBatchEntity batch = productionBatchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Production batch not found: " + id));

        if (batch.getStatus() != ProductionBatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress batches can be completed");
        }

        // Check if all stages are completed
        long pendingStages = productionBatchStageRepository.countByProductionBatchIdAndStatus(id, ProductionBatchStageStatus.PENDING);
        long inProgressStages = productionBatchStageRepository.countByProductionBatchIdAndStatus(id, ProductionBatchStageStatus.IN_PROGRESS);

        if (pendingStages > 0 || inProgressStages > 0) {
            throw new IllegalStateException("All stages must be completed before completing the batch");
        }

        batch.setStatus(ProductionBatchStatus.COMPLETED);
        batch.setCompletedAt(OffsetDateTime.now());

        return productionBatchRepository.save(batch);
    }

    @Override
    public ProductionBatchEntity cancelProductionBatch(Integer id) {
        ProductionBatchEntity batch = productionBatchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Production batch not found: " + id));

        if (batch.getStatus() == ProductionBatchStatus.COMPLETED) {
            throw new IllegalStateException("Completed batches cannot be cancelled");
        }

        batch.setStatus(ProductionBatchStatus.CANCELLED);

        // Cancel all pending stages
        List<ProductionBatchStageEntity> stages = productionBatchStageRepository.findByProductionBatchIdAndStatus(id, ProductionBatchStageStatus.PENDING);
        for (ProductionBatchStageEntity stage : stages) {
            stage.setStatus(ProductionBatchStageStatus.CANCELLED);
        }
        productionBatchStageRepository.saveAll(stages);

        return productionBatchRepository.save(batch);
    }

    @Override
    public boolean deleteProductionBatch(Integer id) {
        Optional<ProductionBatchEntity> batchOpt = productionBatchRepository.findById(id);
        if (batchOpt.isEmpty()) {
            return false;
        }

        ProductionBatchEntity batch = batchOpt.get();
        if (batch.getStatus() == ProductionBatchStatus.IN_PROGRESS || batch.getStatus() == ProductionBatchStatus.COMPLETED) {
            throw new IllegalStateException("Cannot delete in-progress or completed batches");
        }

        productionBatchRepository.deleteById(id);
        return true;
    }

    // ========== Production Batch Stage Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<ProductionBatchStageEntity> findStagesByProductionBatchId(Integer productionBatchId) {
        return productionBatchStageRepository.findByProductionBatchIdWithDetails(productionBatchId);
    }

    @Override
    public ProductionBatchStageEntity completeProductionStage(CompleteProductionStageInput input) {
        ProductionBatchStageEntity stage = productionBatchStageRepository.findById(input.getProductionBatchStageId())
                .orElseThrow(() -> new EntityNotFoundException("Production batch stage not found: " + input.getProductionBatchStageId()));

        if (stage.getStatus() != ProductionBatchStageStatus.PENDING && stage.getStatus() != ProductionBatchStageStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only pending or in-progress stages can be completed");
        }

        stage.setStatus(ProductionBatchStageStatus.COMPLETED);
        stage.setActualQuantity(input.getActualQuantity());
        stage.setCompletedAt(OffsetDateTime.now());
        if (input.getNotes() != null) {
            stage.setNotes(input.getNotes());
        }

        // TODO: Create journal entry for the completed stage
        // This would create the accounting entries based on the technology card stage configuration

        return productionBatchStageRepository.save(stage);
    }

    @Override
    public ProductionBatchStageEntity cancelProductionStage(Integer stageId) {
        ProductionBatchStageEntity stage = productionBatchStageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Production batch stage not found: " + stageId));

        if (stage.getStatus() == ProductionBatchStageStatus.COMPLETED) {
            throw new IllegalStateException("Completed stages cannot be cancelled");
        }

        stage.setStatus(ProductionBatchStageStatus.CANCELLED);
        return productionBatchStageRepository.save(stage);
    }

    // ========== Statistics ==========

    @Override
    @Transactional(readOnly = true)
    public long countTechnologyCardsByCompanyId(Integer companyId) {
        return technologyCardRepository.countByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductionBatchesByCompanyId(Integer companyId) {
        return productionBatchRepository.findByCompanyId(companyId).size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveProductionBatchesByCompanyId(Integer companyId) {
        return productionBatchRepository.countByCompanyIdAndStatus(companyId, ProductionBatchStatus.IN_PROGRESS);
    }

    // ========== Helper Methods ==========

    private TechnologyCardStageEntity createStageFromInput(TechnologyCardEntity card, TechnologyCardStageInput input) {
        AccountEntity inputAccount = accountRepository.findById(input.getInputAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Input account not found: " + input.getInputAccountId()));

        TechnologyCardStageEntity stage = new TechnologyCardStageEntity();
        stage.setTechnologyCard(card);
        stage.setStageOrder(input.getStageOrder());
        stage.setName(input.getName());
        stage.setDescription(input.getDescription());
        stage.setInputAccount(inputAccount);
        stage.setInputQuantity(input.getInputQuantity());
        stage.setInputUnit(input.getInputUnit());
        return stage;
    }
}
