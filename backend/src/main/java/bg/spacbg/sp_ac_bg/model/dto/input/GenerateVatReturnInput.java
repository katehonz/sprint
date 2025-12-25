package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

@Data
public class GenerateVatReturnInput {
    private Integer companyId;
    private Integer periodYear;
    private Integer periodMonth;
}
