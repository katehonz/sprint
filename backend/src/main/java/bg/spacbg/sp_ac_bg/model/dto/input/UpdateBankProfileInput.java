package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.BankImportFormat;
import lombok.Data;

@Data
public class UpdateBankProfileInput {
    private String name;
    private String iban;
    private Integer accountId;
    private Integer bufferAccountId;
    private String currencyCode;
    private BankImportFormat importFormat;
    private Boolean isActive;
    private String settings;
}
