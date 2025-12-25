package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

@Data
public class CreateCurrencyInput {
    private String code;
    private String name;
    private String nameBg;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean isBaseCurrency;
}
