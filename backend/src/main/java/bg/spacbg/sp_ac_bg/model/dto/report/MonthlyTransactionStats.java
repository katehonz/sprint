package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;

public record MonthlyTransactionStats(
    Integer year,
    Integer month,
    String monthName,
    Long totalEntries,
    Long postedEntries,
    Long totalEntryLines,
    Long postedEntryLines,
    BigDecimal totalAmount,
    BigDecimal vatAmount
) {}
