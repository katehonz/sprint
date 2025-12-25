package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFixedAssetCategoryInput {
    private String code;
    private String name;
    private String description;
    private Integer taxCategory;
    private BigDecimal maxTaxDepreciationRate;
    private BigDecimal defaultAccountingDepreciationRate;
    private Integer minUsefulLife;
    private Integer maxUsefulLife;
    private String assetAccountCode;
    private String depreciationAccountCode;
    private String expenseAccountCode;
}
