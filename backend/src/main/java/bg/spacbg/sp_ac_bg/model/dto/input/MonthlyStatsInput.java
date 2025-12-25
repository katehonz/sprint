package bg.spacbg.sp_ac_bg.model.dto.input;

public record MonthlyStatsInput(
    Integer companyId,
    Integer fromYear,
    Integer fromMonth,
    Integer toYear,
    Integer toMonth
) {}
