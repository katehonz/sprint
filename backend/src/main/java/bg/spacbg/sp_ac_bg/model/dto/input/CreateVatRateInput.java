package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateVatRateInput {
    private String code;
    private String name;
    private BigDecimal rate;
    private VatDirection vatDirection;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer companyId;
}
