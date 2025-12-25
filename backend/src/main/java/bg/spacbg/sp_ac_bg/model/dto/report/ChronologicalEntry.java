package bg.spacbg.sp_ac_bg.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChronologicalEntry(
    LocalDate date,
    String debitAccountCode,
    String debitAccountName,
    String creditAccountCode,
    String creditAccountName,
    BigDecimal amount,
    BigDecimal debitCurrencyAmount,
    String debitCurrencyCode,
    BigDecimal creditCurrencyAmount,
    String creditCurrencyCode,
    String documentType,
    LocalDate documentDate,
    String description
) {}
