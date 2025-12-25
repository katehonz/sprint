package bg.spacbg.sp_ac_bg.model.dto;

import bg.spacbg.sp_ac_bg.model.enums.AccountType;
import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartOfAccountsDto {

    private String code;
    private String name;
    private AccountType accountType;
    private Integer accountClass;
    private String parentCode;
    private Boolean isAnalytical;
    private Boolean isVatApplicable;
    private VatDirection vatDirection;
    private Boolean supportsQuantities;
    private String defaultUnit;
    private Boolean isActive;

    // For nested children export
    private List<ChartOfAccountsDto> children;
}
