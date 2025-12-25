package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateEntryLineInput {
    private Integer accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private Integer counterpartId;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private BigDecimal vatAmount;
    private BigDecimal quantity;
    private String unitOfMeasureCode;
    private String description;
    private Integer lineOrder;
}
