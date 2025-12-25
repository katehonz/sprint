package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStatus;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProductionBatchFilterInput {
    private Integer companyId;
    private ProductionBatchStatus status;
    private Integer technologyCardId;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
}
