package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @Column(name = "role", length = 50)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "log_login")
    private Boolean logLogin = true;

    @Column(name = "log_logout")
    private Boolean logLogout = true;

    @Column(name = "log_view_reports")
    private Boolean logViewReports = true;

    @Column(name = "log_generate_reports")
    private Boolean logGenerateReports = true;

    @Column(name = "log_export_data")
    private Boolean logExportData = true;

    @Column(name = "log_create")
    private Boolean logCreate = false;

    @Column(name = "log_update")
    private Boolean logUpdate = false;

    @Column(name = "log_delete")
    private Boolean logDelete = true;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
