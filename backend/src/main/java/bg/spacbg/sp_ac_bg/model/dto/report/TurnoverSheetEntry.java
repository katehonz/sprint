package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;

public record TurnoverSheetEntry(
    Integer accountId,
    String accountCode,
    String accountName,
    BigDecimal openingDebit,
    BigDecimal openingCredit,
    BigDecimal periodDebit,
    BigDecimal periodCredit,
    BigDecimal closingDebit,
    BigDecimal closingCredit
) {}
