package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "salt_edge_connections")
public class SaltEdgeConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_profile_id", nullable = false)
    private BankProfileEntity bankProfile;

    @Column(name = "salt_edge_connection_id", nullable = false, unique = true)
    private String saltEdgeConnectionId;

    @Column(name = "salt_edge_customer_id", nullable = false)
    private String saltEdgeCustomerId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(nullable = false)
    private String status = "pending"; // active, inactive, disabled, pending

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "next_refresh_possible_at")
    private OffsetDateTime nextRefreshPossibleAt;

    @Column(name = "daily_refresh")
    private Boolean dailyRefresh = false;

    @Column(name = "consent_id")
    private String consentId;

    @Column(name = "consent_expires_at")
    private OffsetDateTime consentExpiresAt;

    @Column(name = "error_class")
    private String errorClass;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public boolean hasValidConsent() {
        return consentExpiresAt != null && consentExpiresAt.isAfter(OffsetDateTime.now());
    }
}
