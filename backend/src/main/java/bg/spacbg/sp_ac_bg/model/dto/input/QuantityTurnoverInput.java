package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;

/**
 * Входни параметри за количествена оборотна ведомост
 */
@Data
public class QuantityTurnoverInput {
    private Integer companyId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
