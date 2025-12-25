package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record BgLedgerByCredit(
    String creditAccountCode,
    String creditAccountName,
    List<BgLedgerByCreditEntry> entries,
    BigDecimal totalAmount
) {}
