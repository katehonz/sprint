package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VatDirection {
    NONE("NONE"),
    INPUT("INPUT"),
    OUTPUT("OUTPUT"),
    BOTH("BOTH");

    private final String value;

    VatDirection(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VatDirection fromValue(String value) {
        for (VatDirection direction : VatDirection.values()) {
            if (direction.value.equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown VatDirection value: " + value);
    }
}
