package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SaltEdgeWebhookPayload {
    private WebhookData data;
    private WebhookMeta meta;

    @Data
    public static class WebhookData {
        @JsonProperty("connection_id")
        private String connectionId;

        @JsonProperty("customer_id")
        private String customerId;

        private String stage; // start, connect, interactive, fetch_accounts, fetch_recent, fetch_full, finish, error

        @JsonProperty("provider_code")
        private String providerCode;

        @JsonProperty("error_class")
        private String errorClass;

        @JsonProperty("error_message")
        private String errorMessage;

        private Map<String, Object> extra;
    }

    @Data
    public static class WebhookMeta {
        private String version;

        @JsonProperty("time")
        private String time;
    }
}
