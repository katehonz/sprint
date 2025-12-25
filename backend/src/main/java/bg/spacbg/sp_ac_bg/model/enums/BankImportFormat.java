package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BankImportFormat {
    UNICREDIT_MT940("UNICREDIT_MT940"),
    WISE_CAMT053("WISE_CAMT053"),
    REVOLUT_CAMT053("REVOLUT_CAMT053"),
    PAYSERA_CAMT053("PAYSERA_CAMT053"),
    POSTBANK_XML("POSTBANK_XML"),
    OBB_XML("OBB_XML"),
    CCB_CSV("CCB_CSV");

    private final String value;

    BankImportFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BankImportFormat fromValue(String value) {
        for (BankImportFormat format : BankImportFormat.values()) {
            if (format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported bank import format: " + value);
    }
}
