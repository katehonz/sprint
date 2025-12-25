package bg.spacbg.sp_ac_bg.model.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TurnoverSheet(
    String companyName,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<TurnoverSheetEntry> entries,
    TurnoverSheetEntry totals,
    OffsetDateTime generatedAt
) {}
