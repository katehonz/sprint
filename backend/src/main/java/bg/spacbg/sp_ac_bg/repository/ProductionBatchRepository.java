package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.ProductionBatchEntity;
import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatchEntity, Integer> {

    List<ProductionBatchEntity> findByCompanyId(Integer companyId);

    Page<ProductionBatchEntity> findByCompanyId(Integer companyId, Pageable pageable);

    List<ProductionBatchEntity> findByCompanyIdAndStatus(Integer companyId, ProductionBatchStatus status);

    Optional<ProductionBatchEntity> findByCompanyIdAndBatchNumber(Integer companyId, String batchNumber);

    boolean existsByCompanyIdAndBatchNumber(Integer companyId, String batchNumber);

    @Query("SELECT pb FROM ProductionBatchEntity pb " +
           "LEFT JOIN FETCH pb.stages " +
           "LEFT JOIN FETCH pb.technologyCard " +
           "WHERE pb.id = :id")
    Optional<ProductionBatchEntity> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT pb FROM ProductionBatchEntity pb " +
           "WHERE pb.company.id = :companyId " +
           "AND (:status IS NULL OR pb.status = :status) " +
           "AND (:technologyCardId IS NULL OR pb.technologyCard.id = :technologyCardId) " +
           "AND (:startDate IS NULL OR pb.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR pb.createdAt <= :endDate) " +
           "ORDER BY pb.createdAt DESC")
    List<ProductionBatchEntity> findByFilters(
            @Param("companyId") Integer companyId,
            @Param("status") ProductionBatchStatus status,
            @Param("technologyCardId") Integer technologyCardId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT COUNT(pb) FROM ProductionBatchEntity pb WHERE pb.company.id = :companyId AND pb.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Integer companyId, @Param("status") ProductionBatchStatus status);

    @Query("SELECT pb FROM ProductionBatchEntity pb WHERE pb.technologyCard.id = :technologyCardId")
    List<ProductionBatchEntity> findByTechnologyCardId(@Param("technologyCardId") Integer technologyCardId);
}
