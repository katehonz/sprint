package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.AccountType;
import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
import lombok.Data;

@Data
public class CreateAccountInput {
    private String code;
    private String name;
    private String description;
    private AccountType accountType;
    private Integer accountClass;
    private Integer parentId;
    private Boolean isVatApplicable;
    private VatDirection vatDirection;
    private Boolean supportsQuantities;
    private String defaultUnit;
    private Integer companyId;
}
