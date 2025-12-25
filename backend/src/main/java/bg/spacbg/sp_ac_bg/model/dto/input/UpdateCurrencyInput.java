package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

@Data
public class UpdateCurrencyInput {
    private String name;
    private String nameBg;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean isActive;
}
