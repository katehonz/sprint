package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowDirection {
    ARRIVAL("ARRIVAL"),
    DISPATCH("DISPATCH");

    private final String value;

    FlowDirection(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FlowDirection fromValue(String value) {
        for (FlowDirection direction : FlowDirection.values()) {
            if (direction.value.equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown FlowDirection value: " + value);
    }
}
