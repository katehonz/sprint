package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.VatRateEntity;
import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VatRateRepository extends JpaRepository<VatRateEntity, Integer> {

    List<VatRateEntity> findByCompanyId(Integer companyId);

    List<VatRateEntity> findByCompanyIdAndIsActiveTrue(Integer companyId);

    Optional<VatRateEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT v FROM VatRateEntity v WHERE v.company.id = :companyId " +
           "AND v.isActive = true " +
           "AND v.validFrom <= :date " +
           "AND (v.validTo IS NULL OR v.validTo >= :date)")
    List<VatRateEntity> findActiveRatesForDate(Integer companyId, LocalDate date);

    @Query("SELECT v FROM VatRateEntity v WHERE v.company.id = :companyId " +
           "AND v.vatDirection = :direction " +
           "AND v.isActive = true")
    List<VatRateEntity> findByCompanyIdAndVatDirection(Integer companyId, VatDirection direction);
}
