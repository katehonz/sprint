package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BankImportStatus {
    COMPLETED("completed"),
    FAILED("failed"),
    IN_PROGRESS("in_progress");

    private final String value;

    BankImportStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BankImportStatus fromValue(String value) {
        for (BankImportStatus status : BankImportStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BankImportStatus value: " + value);
    }
}
