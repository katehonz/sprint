package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SaltEdgeCustomer {
    private String id;
    private String identifier;
    private String secret;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
