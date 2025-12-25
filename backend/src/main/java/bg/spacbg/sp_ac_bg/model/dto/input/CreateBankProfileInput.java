package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.BankConnectionType;
import bg.spacbg.sp_ac_bg.model.enums.BankImportFormat;
import lombok.Data;

@Data
public class CreateBankProfileInput {
    private Integer companyId;
    private String name;
    private String iban;
    private Integer accountId;
    private Integer bufferAccountId;
    private String currencyCode;

    // Connection type: FILE_IMPORT (default), SALT_EDGE, or MANUAL
    private BankConnectionType connectionType = BankConnectionType.FILE_IMPORT;

    // For file import
    private BankImportFormat importFormat;

    // For Salt Edge Open Banking
    private String saltEdgeProviderCode;
    private String saltEdgeAccountId;

    private String settings;
}
