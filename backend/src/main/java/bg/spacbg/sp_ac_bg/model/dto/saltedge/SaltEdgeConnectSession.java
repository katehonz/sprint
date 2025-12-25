package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SaltEdgeConnectSession {
    @JsonProperty("connect_url")
    private String connectUrl;

    @JsonProperty("expires_at")
    private OffsetDateTime expiresAt;
}
