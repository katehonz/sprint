package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeclarationType {
    ARRIVAL("ARRIVAL"),
    DISPATCH("DISPATCH");

    private final String value;

    DeclarationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeclarationType fromValue(String value) {
        for (DeclarationType type : DeclarationType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DeclarationType value: " + value);
    }
}
