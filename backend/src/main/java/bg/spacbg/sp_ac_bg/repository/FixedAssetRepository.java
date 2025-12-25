package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAssetEntity, Integer> {

    List<FixedAssetEntity> findByCompanyId(Integer companyId);

    List<FixedAssetEntity> findByCategoryId(Integer categoryId);

    Optional<FixedAssetEntity> findByInventoryNumber(String inventoryNumber);

    boolean existsByInventoryNumber(String inventoryNumber);

    List<FixedAssetEntity> findByCompanyIdAndStatus(Integer companyId, String status);

    @Query("SELECT f FROM FixedAssetEntity f WHERE f.company.id = :companyId " +
           "AND f.status = 'ACTIVE' " +
           "AND f.putIntoServiceDate IS NOT NULL " +
           "AND f.putIntoServiceDate <= :toDate")
    List<FixedAssetEntity> findActiveAssetsForDepreciation(Integer companyId, LocalDate toDate);

    @Query("SELECT f FROM FixedAssetEntity f WHERE f.company.id = :companyId " +
           "AND f.status = 'ACTIVE' " +
           "AND f.acquisitionDate BETWEEN :fromDate AND :toDate")
    List<FixedAssetEntity> findAcquiredInPeriod(Integer companyId, LocalDate fromDate, LocalDate toDate);
}
