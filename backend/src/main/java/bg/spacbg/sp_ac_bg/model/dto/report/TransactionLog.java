package bg.spacbg.sp_ac_bg.model.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionLog(
    String companyName,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<TransactionLogEntry> entries,
    OffsetDateTime generatedAt
) {}
