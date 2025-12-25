package bg.spacbg.sp_ac_bg.model.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Количествена оборотна ведомост - ред за сметка
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantityTurnoverDto {
    private Integer accountId;
    private String accountCode;
    private String accountName;

    // Начално салдо
    private BigDecimal openingQuantity;
    private BigDecimal openingAmount;

    // Постъпване (Дебит)
    private BigDecimal receiptQuantity;
    private BigDecimal receiptAmount;

    // Разход (Кредит)
    private BigDecimal issueQuantity;
    private BigDecimal issueAmount;

    // Крайно салдо
    private BigDecimal closingQuantity;
    private BigDecimal closingAmount;
}
