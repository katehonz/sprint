package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.UpdateSmtpSettingsInput;
import bg.spacbg.sp_ac_bg.model.entity.SystemSettingsEntity;

public interface SystemSettingsService {

    SystemSettingsEntity getSettings();

    SystemSettingsEntity updateSmtpSettings(UpdateSmtpSettingsInput input, String updatedBy);

    boolean testSmtpConnection(String testEmail);
}
