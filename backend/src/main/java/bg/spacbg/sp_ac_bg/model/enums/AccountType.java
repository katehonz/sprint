package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountType {
    ASSET("ASSET"),
    LIABILITY("LIABILITY"),
    EQUITY("EQUITY"),
    REVENUE("REVENUE"),
    EXPENSE("EXPENSE");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AccountType fromValue(String value) {
        for (AccountType type : AccountType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AccountType value: " + value);
    }
}
