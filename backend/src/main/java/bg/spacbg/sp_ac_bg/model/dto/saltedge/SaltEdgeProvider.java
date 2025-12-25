package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SaltEdgeProvider {
    private String id;
    private String code;
    private String name;

    @JsonProperty("country_code")
    private String countryCode;

    private String status; // active, inactive, disabled

    @JsonProperty("automatic_fetch")
    private Boolean automaticFetch;

    @JsonProperty("customer_notified_on_sign_in")
    private Boolean customerNotifiedOnSignIn;

    private Boolean interactive;

    @JsonProperty("identification_mode")
    private String identificationMode;

    @JsonProperty("instruction")
    private String instruction;

    @JsonProperty("home_url")
    private String homeUrl;

    @JsonProperty("login_url")
    private String loginUrl;

    @JsonProperty("logo_url")
    private String logoUrl;

    @JsonProperty("max_consent_days")
    private Integer maxConsentDays;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("holder_info")
    private List<String> holderInfo;

    @JsonProperty("required_fields")
    private List<ProviderField> requiredFields;

    @Data
    public static class ProviderField {
        private String name;
        private String nature; // text, password, select, etc.

        @JsonProperty("localized_name")
        private String localizedName;

        private Boolean optional;
        private Integer position;

        @JsonProperty("field_options")
        private List<FieldOption> fieldOptions;
    }

    @Data
    public static class FieldOption {
        private String name;

        @JsonProperty("localized_name")
        private String localizedName;

        private String value;
        private Boolean selected;
    }
}
