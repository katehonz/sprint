package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.AccountingPeriodFilter;
import bg.spacbg.sp_ac_bg.model.dto.input.CloseAccountingPeriodInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountingPeriodEntity;
import bg.spacbg.sp_ac_bg.model.enums.PeriodStatus;
import bg.spacbg.sp_ac_bg.service.AccountingPeriodService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    public AccountingPeriodController(AccountingPeriodService accountingPeriodService) {
        this.accountingPeriodService = accountingPeriodService;
    }

    // ========== Queries ==========

    @QueryMapping
    public List<AccountingPeriodEntity> accountingPeriods(@Argument Integer companyId) {
        return accountingPeriodService.findByCompanyId(companyId);
    }

    @QueryMapping
    public List<AccountingPeriodEntity> accountingPeriodsByFilter(@Argument AccountingPeriodFilter filter) {
        return accountingPeriodService.findByFilter(filter);
    }

    @QueryMapping
    public AccountingPeriodEntity accountingPeriod(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        return accountingPeriodService.findByCompanyIdAndYearAndMonth(companyId, year, month).orElse(null);
    }

    @QueryMapping
    public Boolean isPeriodOpen(
            @Argument Integer companyId,
            @Argument Integer year,
            @Argument Integer month) {
        LocalDate date = LocalDate.of(year, month, 1);
        return accountingPeriodService.isPeriodOpen(companyId, date);
    }

    // ========== Mutations ==========

    @MutationMapping
    public AccountingPeriodEntity closeAccountingPeriod(@Argument CloseAccountingPeriodInput input) {
        Integer userId = getCurrentUserId();
        return accountingPeriodService.closePeriod(
                input.getCompanyId(),
                input.getYear(),
                input.getMonth(),
                userId);
    }

    @MutationMapping
    public AccountingPeriodEntity reopenAccountingPeriod(@Argument CloseAccountingPeriodInput input) {
        return accountingPeriodService.reopenPeriod(
                input.getCompanyId(),
                input.getYear(),
                input.getMonth());
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from authentication
        return 1; // Placeholder
    }
}
