package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionLogEntry(
    LocalDate date,
    String entryNumber,
    String documentNumber,
    String description,
    String accountCode,
    String accountName,
    BigDecimal debitAmount,
    BigDecimal creditAmount,
    String counterpartName
) {}
