package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RateSource {
    ECB("ECB"),
    MANUAL("MANUAL"),
    API("API");

    private final String value;

    RateSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RateSource fromValue(String value) {
        for (RateSource source : RateSource.values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown RateSource value: " + value);
    }
}
