package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GeneralLedgerEntry(
    LocalDate date,
    String entryNumber,
    String documentNumber,
    String description,
    BigDecimal debitAmount,
    BigDecimal creditAmount,
    BigDecimal balance,
    String counterpartName
) {}
