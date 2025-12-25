package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.AccountingPeriodFilter;
import bg.spacbg.sp_ac_bg.model.entity.AccountingPeriodEntity;
import bg.spacbg.sp_ac_bg.model.enums.PeriodStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodService {

    List<AccountingPeriodEntity> findByCompanyId(Integer companyId);

    List<AccountingPeriodEntity> findByFilter(AccountingPeriodFilter filter);

    Optional<AccountingPeriodEntity> findById(Integer id);

    Optional<AccountingPeriodEntity> findByCompanyIdAndYearAndMonth(Integer companyId, Integer year, Integer month);

    AccountingPeriodEntity closePeriod(Integer companyId, Integer year, Integer month, Integer userId);

    AccountingPeriodEntity reopenPeriod(Integer companyId, Integer year, Integer month);

    boolean isPeriodOpen(Integer companyId, LocalDate date);

    void validatePeriodIsOpen(Integer companyId, LocalDate date);

    PeriodStatus getPeriodStatus(Integer companyId, Integer year, Integer month);
}
