package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.VatReturnEntity;
import bg.spacbg.sp_ac_bg.model.enums.VatReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VatReturnRepository extends JpaRepository<VatReturnEntity, Integer> {

    List<VatReturnEntity> findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(Integer companyId);

    Optional<VatReturnEntity> findByCompanyIdAndPeriodYearAndPeriodMonth(
            Integer companyId, Integer periodYear, Integer periodMonth);

    List<VatReturnEntity> findByCompanyIdAndStatus(Integer companyId, VatReturnStatus status);

    boolean existsByCompanyIdAndPeriodYearAndPeriodMonth(
            Integer companyId, Integer periodYear, Integer periodMonth);
}
