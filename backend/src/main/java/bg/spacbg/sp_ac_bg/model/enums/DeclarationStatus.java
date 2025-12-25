package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeclarationStatus {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED");

    private final String value;

    DeclarationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeclarationStatus fromValue(String value) {
        for (DeclarationStatus status : DeclarationStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown DeclarationStatus value: " + value);
    }
}
