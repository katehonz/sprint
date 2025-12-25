package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ChronologicalReport(
    String companyName,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<ChronologicalEntry> entries,
    BigDecimal totalAmount,
    OffsetDateTime generatedAt
) {}
