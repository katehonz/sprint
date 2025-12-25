package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;

public record BgLedgerByDebitEntry(
    String creditAccountCode,
    String creditAccountName,
    BigDecimal amount
) {}
