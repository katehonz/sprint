package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RateProvider {
    ECB("ECB"),
    MANUAL("MANUAL"),
    API("API");

    private final String value;

    RateProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RateProvider fromValue(String value) {
        for (RateProvider provider : RateProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown RateProvider value: " + value);
    }
}
