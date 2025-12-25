package bg.spacbg.sp_ac_bg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "saltedge")
public class SaltEdgeConfig {

    private String appId;
    private String secret;
    private String baseUrl = "https://www.saltedge.com/api/v5";
    private String callbackUrl;
    private String returnUrl;

    // Provider codes for Bulgarian banks supported by Salt Edge
    public static final String PROVIDER_UNICREDIT_BG = "unicredit_bulbank_bg";
    public static final String PROVIDER_DSK_BG = "dsk_bank_bg";
    public static final String PROVIDER_POSTBANK_BG = "postbank_bg";
    public static final String PROVIDER_FIBANK_BG = "first_investment_bank_bg";
    public static final String PROVIDER_UBB_BG = "ubb_bg";
    public static final String PROVIDER_RAIFFEISEN_BG = "raiffeisen_bank_bg";
    public static final String PROVIDER_OBB_BG = "obb_bg";
    public static final String PROVIDER_CCB_BG = "ccb_bg";

    // International banks
    public static final String PROVIDER_REVOLUT = "revolut_eu";
    public static final String PROVIDER_WISE = "wise_eu";
    public static final String PROVIDER_N26 = "n26_de";
    public static final String PROVIDER_PAYSERA = "paysera_lt";
}
