package bg.spacbg.sp_ac_bg.model.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Информация за средно претеглена цена на материална сметка
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageCostInfoDto {
    private Integer accountId;
    private BigDecimal currentQuantity;
    private BigDecimal currentAmount;
    private BigDecimal averageCost;
}
