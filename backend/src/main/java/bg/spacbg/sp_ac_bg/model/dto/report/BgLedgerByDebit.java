package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record BgLedgerByDebit(
    String debitAccountCode,
    String debitAccountName,
    List<BgLedgerByDebitEntry> entries,
    BigDecimal totalAmount
) {}
