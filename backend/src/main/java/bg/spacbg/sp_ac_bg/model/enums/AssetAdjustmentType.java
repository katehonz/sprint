package bg.spacbg.sp_ac_bg.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetAdjustmentType {
    ACQUISITION("acquisition"),
    IMPROVEMENT("improvement"),
    IMPAIRMENT("impairment"),
    REVALUATION("revaluation"),
    DISPOSAL("disposal"),
    INTERNAL_TRANSFER("internal_transfer"),
    SCRAP("scrap");

    private final String value;

    AssetAdjustmentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AssetAdjustmentType fromValue(String value) {
        for (AssetAdjustmentType type : AssetAdjustmentType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AssetAdjustmentType value: " + value);
    }
}
