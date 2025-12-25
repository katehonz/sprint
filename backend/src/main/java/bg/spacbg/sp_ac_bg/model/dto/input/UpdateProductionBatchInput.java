package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductionBatchInput {
    private Integer id;
    private String batchNumber;
    private BigDecimal plannedQuantity;
    private BigDecimal actualQuantity;
    private ProductionBatchStatus status;
    private String notes;
}
