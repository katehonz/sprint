package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PeriodStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED");

    private final String value;

    PeriodStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PeriodStatus fromValue(String value) {
        for (PeriodStatus status : PeriodStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PeriodStatus value: " + value);
    }
}
