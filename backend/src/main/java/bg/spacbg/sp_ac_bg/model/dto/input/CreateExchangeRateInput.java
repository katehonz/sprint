package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.RateSource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateExchangeRateInput {
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal rate;
    private LocalDate validDate;
    private RateSource rateSource;
    private String notes;
}
