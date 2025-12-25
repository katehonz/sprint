package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VatReturnStatus {
    DRAFT("DRAFT"),
    CALCULATED("CALCULATED"),
    SUBMITTED("SUBMITTED"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED");

    private final String value;

    VatReturnStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VatReturnStatus fromValue(String value) {
        for (VatReturnStatus status : VatReturnStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VatReturnStatus value: " + value);
    }
}
