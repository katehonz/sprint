package bg.spacbg.sp_ac_bg.model.dto.input;

import java.time.LocalDate;

public record TurnoverReportInput(
    Integer companyId,
    LocalDate startDate,
    LocalDate endDate,
    Integer accountId,
    Boolean showZeroBalances,
    Integer accountCodeDepth
) {}
