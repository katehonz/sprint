package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BankConnectionType {
    FILE_IMPORT("FILE_IMPORT"),
    SALT_EDGE("SALT_EDGE"),
    MANUAL("MANUAL");

    private final String value;

    BankConnectionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BankConnectionType fromValue(String value) {
        for (BankConnectionType type : BankConnectionType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown bank connection type: " + value);
    }
}
