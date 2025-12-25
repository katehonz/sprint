package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.DepreciationJournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepreciationJournalRepository extends JpaRepository<DepreciationJournalEntity, Integer> {

    List<DepreciationJournalEntity> findByCompanyIdAndPeriod(Integer companyId, LocalDate period);

    List<DepreciationJournalEntity> findByFixedAssetId(Integer fixedAssetId);

    Optional<DepreciationJournalEntity> findByFixedAssetIdAndPeriod(Integer fixedAssetId, LocalDate period);

    List<DepreciationJournalEntity> findByCompanyIdAndIsPostedFalse(Integer companyId);

    List<DepreciationJournalEntity> findByCompanyIdAndPeriodAndIsPostedFalse(Integer companyId, LocalDate period);

    @Query("SELECT DISTINCT d.period FROM DepreciationJournalEntity d " +
           "WHERE d.company.id = :companyId ORDER BY d.period")
    List<LocalDate> findDistinctPeriodsByCompanyId(Integer companyId);

    @Query("SELECT DISTINCT d.period FROM DepreciationJournalEntity d " +
           "WHERE d.company.id = :companyId AND d.isPosted = true ORDER BY d.period")
    List<LocalDate> findDistinctPostedPeriodsByCompanyId(Integer companyId);

    @Query("SELECT d FROM DepreciationJournalEntity d " +
           "WHERE d.company.id = :companyId " +
           "AND YEAR(d.period) = :year " +
           "ORDER BY d.period, d.fixedAsset.name")
    List<DepreciationJournalEntity> findByCompanyIdAndYear(Integer companyId, Integer year);

    @Query("SELECT d FROM DepreciationJournalEntity d " +
           "WHERE d.company.id = :companyId " +
           "AND YEAR(d.period) = :year AND MONTH(d.period) = :month " +
           "ORDER BY d.fixedAsset.name")
    List<DepreciationJournalEntity> findByCompanyIdAndYearAndMonth(Integer companyId, Integer year, Integer month);

    boolean existsByFixedAssetIdAndPeriod(Integer fixedAssetId, LocalDate period);
}
