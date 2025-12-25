package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductionBatchInput {
    private Integer companyId;
    private Integer technologyCardId;
    private String batchNumber;
    private BigDecimal plannedQuantity;
    private String notes;
}
