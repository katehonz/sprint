package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateTechnologyCardInput {
    private Integer companyId;
    private String code;
    private String name;
    private String description;
    private Integer outputAccountId;
    private BigDecimal outputQuantity;
    private String outputUnit;
    private List<TechnologyCardStageInput> stages;
}
