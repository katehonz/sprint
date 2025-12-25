package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.UpdateSmtpSettingsInput;
import bg.spacbg.sp_ac_bg.model.entity.SystemSettingsEntity;
import bg.spacbg.sp_ac_bg.service.SystemSettingsService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public SystemSettingsEntity systemSettings() {
        return systemSettingsService.getSettings();
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SystemSettingsEntity updateSmtpSettings(@Argument UpdateSmtpSettingsInput input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return systemSettingsService.updateSmtpSettings(input, username);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public boolean testSmtpConnection(@Argument String testEmail) {
        return systemSettingsService.testSmtpConnection(testEmail);
    }
}
