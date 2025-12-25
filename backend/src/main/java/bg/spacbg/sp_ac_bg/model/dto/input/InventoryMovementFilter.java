package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryMovementFilter {
    private Integer companyId;
    private Integer accountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String movementType;
}
