package bg.spacbg.sp_ac_bg.model.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Необходима корекция на СПЦ при ретроактивен запис
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionNeededDto {
    private Integer movementId;
    private LocalDate movementDate;
    private Integer materialAccountId;
    private String materialAccountCode;
    private String materialAccountName;
    private Integer expenseAccountId;
    private String expenseAccountCode;
    private String expenseAccountName;
    private BigDecimal quantity;
    private BigDecimal oldAverageCost;
    private BigDecimal newAverageCost;
    private BigDecimal correctionAmount;
    private String description;
}
