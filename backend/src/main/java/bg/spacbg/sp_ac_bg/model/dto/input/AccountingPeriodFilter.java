package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.PeriodStatus;

public class AccountingPeriodFilter {
    private Integer companyId;
    private Integer year;
    private PeriodStatus status;

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
    }
}
