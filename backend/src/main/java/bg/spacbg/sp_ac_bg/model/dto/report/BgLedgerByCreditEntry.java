package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;

public record BgLedgerByCreditEntry(
    String debitAccountCode,
    String debitAccountName,
    BigDecimal amount
) {}
