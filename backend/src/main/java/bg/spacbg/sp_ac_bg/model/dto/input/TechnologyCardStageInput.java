package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TechnologyCardStageInput {
    private Integer id; // null for new stages
    private Integer stageOrder;
    private String name;
    private String description;
    private Integer inputAccountId;
    private BigDecimal inputQuantity;
    private String inputUnit;
}
