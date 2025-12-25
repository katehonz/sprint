package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Глобални системни настройки - единствен запис в таблицата
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity {

    @Id
    private Integer id = 1; // Винаги е 1 - единствен запис

    // ========== SMTP настройки ==========

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword; // TODO: Encrypt in database

    @Column(name = "smtp_from_email")
    private String smtpFromEmail;

    @Column(name = "smtp_from_name")
    private String smtpFromName;

    @Column(name = "smtp_use_tls", nullable = false)
    private boolean smtpUseTls = true;

    @Column(name = "smtp_use_ssl", nullable = false)
    private boolean smtpUseSsl = false;

    @Column(name = "smtp_enabled", nullable = false)
    private boolean smtpEnabled = false;

    // ========== Други глобални настройки (за бъдещо разширение) ==========

    @Column(name = "app_name")
    private String appName = "SP-AC Accounting";

    @Column(name = "default_language")
    private String defaultLanguage = "bg";

    @Column(name = "default_timezone")
    private String defaultTimezone = "Europe/Sofia";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
