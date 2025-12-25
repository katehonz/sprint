package bg.spacbg.sp_ac_bg.model.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GeneralLedger(
    String companyName,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<GeneralLedgerAccount> accounts,
    OffsetDateTime generatedAt
) {}
