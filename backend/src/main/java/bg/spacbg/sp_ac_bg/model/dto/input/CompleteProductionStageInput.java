package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteProductionStageInput {
    private Integer productionBatchStageId;
    private BigDecimal actualQuantity;
    private String notes;
}
