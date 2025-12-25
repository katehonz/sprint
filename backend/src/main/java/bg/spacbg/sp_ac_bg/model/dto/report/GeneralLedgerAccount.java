package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record GeneralLedgerAccount(
    Integer accountId,
    String accountCode,
    String accountName,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    List<GeneralLedgerEntry> entries
) {}
