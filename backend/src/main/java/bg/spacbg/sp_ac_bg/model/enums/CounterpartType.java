package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CounterpartType {
    CUSTOMER("CUSTOMER"),
    SUPPLIER("SUPPLIER"),
    EMPLOYEE("EMPLOYEE"),
    BANK("BANK"),
    GOVERNMENT("GOVERNMENT"),
    OTHER("OTHER");

    private final String value;

    CounterpartType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CounterpartType fromValue(String value) {
        for (CounterpartType type : CounterpartType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CounterpartType value: " + value);
    }
}
