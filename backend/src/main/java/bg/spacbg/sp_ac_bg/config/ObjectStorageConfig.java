package bg.spacbg.sp_ac_bg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "object-storage")
public class ObjectStorageConfig {
    private boolean enabled;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String prefix;
    private boolean forcePathStyle = true;
}
