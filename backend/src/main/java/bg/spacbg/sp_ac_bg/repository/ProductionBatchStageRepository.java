package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchStageEntity;
import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionBatchStageRepository extends JpaRepository<ProductionBatchStageEntity, Integer> {

    List<ProductionBatchStageEntity> findByProductionBatchId(Integer productionBatchId);

    List<ProductionBatchStageEntity> findByProductionBatchIdOrderByTechnologyCardStageStageOrderAsc(Integer productionBatchId);

    List<ProductionBatchStageEntity> findByProductionBatchIdAndStatus(Integer productionBatchId, ProductionBatchStageStatus status);

    @Query("SELECT pbs FROM ProductionBatchStageEntity pbs " +
           "LEFT JOIN FETCH pbs.technologyCardStage " +
           "LEFT JOIN FETCH pbs.journalEntry " +
           "WHERE pbs.productionBatch.id = :productionBatchId " +
           "ORDER BY pbs.technologyCardStage.stageOrder ASC")
    List<ProductionBatchStageEntity> findByProductionBatchIdWithDetails(@Param("productionBatchId") Integer productionBatchId);

    @Query("SELECT pbs FROM ProductionBatchStageEntity pbs " +
           "WHERE pbs.productionBatch.id = :productionBatchId " +
           "AND pbs.status = 'PENDING' " +
           "ORDER BY pbs.technologyCardStage.stageOrder ASC")
    Optional<ProductionBatchStageEntity> findNextPendingStage(@Param("productionBatchId") Integer productionBatchId);

    void deleteByProductionBatchId(Integer productionBatchId);

    @Query("SELECT COUNT(pbs) FROM ProductionBatchStageEntity pbs " +
           "WHERE pbs.productionBatch.id = :productionBatchId AND pbs.status = :status")
    long countByProductionBatchIdAndStatus(
            @Param("productionBatchId") Integer productionBatchId,
            @Param("status") ProductionBatchStageStatus status);
}
