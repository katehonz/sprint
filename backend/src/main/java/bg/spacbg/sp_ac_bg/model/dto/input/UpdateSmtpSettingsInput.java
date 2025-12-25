package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

@Data
public class UpdateSmtpSettingsInput {
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpFromEmail;
    private String smtpFromName;
    private Boolean smtpUseTls;
    private Boolean smtpUseSsl;
    private Boolean smtpEnabled;
}
