package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.AccountingPeriodEntity;
import bg.spacbg.sp_ac_bg.model.enums.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriodEntity, Integer> {

    List<AccountingPeriodEntity> findByCompany_IdOrderByYearDescMonthDesc(Integer companyId);

    Optional<AccountingPeriodEntity> findByCompany_IdAndYearAndMonth(
            Integer companyId, Integer year, Integer month);

    List<AccountingPeriodEntity> findByCompany_IdAndStatus(Integer companyId, PeriodStatus status);

    List<AccountingPeriodEntity> findByCompany_IdAndYear(Integer companyId, Integer year);

    boolean existsByCompany_IdAndYearAndMonth(Integer companyId, Integer year, Integer month);
}
