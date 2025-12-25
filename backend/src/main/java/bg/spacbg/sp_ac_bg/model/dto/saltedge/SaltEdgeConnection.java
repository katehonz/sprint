package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class SaltEdgeConnection {
    private String id;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("provider_id")
    private String providerId;

    @JsonProperty("provider_code")
    private String providerCode;

    @JsonProperty("provider_name")
    private String providerName;

    private String status; // active, inactive, disabled

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("last_success_at")
    private OffsetDateTime lastSuccessAt;

    @JsonProperty("next_refresh_possible_at")
    private OffsetDateTime nextRefreshPossibleAt;

    @JsonProperty("daily_refresh")
    private Boolean dailyRefresh;

    @JsonProperty("show_consent_confirmation")
    private Boolean showConsentConfirmation;

    @JsonProperty("last_consent_id")
    private String lastConsentId;

    @JsonProperty("last_attempt")
    private Map<String, Object> lastAttempt;
}
