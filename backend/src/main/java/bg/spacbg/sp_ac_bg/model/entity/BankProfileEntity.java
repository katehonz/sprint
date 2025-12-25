package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.BankConnectionType;
import bg.spacbg.sp_ac_bg.model.enums.BankImportFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bank_profiles")
public class BankProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String name;

    private String iban;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffer_account_id", nullable = false)
    private AccountEntity bufferAccount;

    @Column(nullable = false)
    private String currencyCode;

    // Connection type: FILE_IMPORT, SALT_EDGE, or MANUAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankConnectionType connectionType = BankConnectionType.FILE_IMPORT;

    // File import format (nullable for Open Banking connections)
    @Enumerated(EnumType.STRING)
    private BankImportFormat importFormat;

    // Salt Edge Open Banking fields
    @Column(name = "salt_edge_connection_id")
    private String saltEdgeConnectionId;

    @Column(name = "salt_edge_account_id")
    private String saltEdgeAccountId;

    @Column(name = "salt_edge_provider_code")
    private String saltEdgeProviderCode;

    @Column(name = "salt_edge_provider_name")
    private String saltEdgeProviderName;

    @Column(name = "salt_edge_last_sync_at")
    private OffsetDateTime saltEdgeLastSyncAt;

    @Column(name = "salt_edge_consent_expires_at")
    private OffsetDateTime saltEdgeConsentExpiresAt;

    @Column(name = "salt_edge_status")
    private String saltEdgeStatus;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(columnDefinition = "jsonb")
    private String settings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @OneToMany(mappedBy = "bankProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankImportEntity> bankImports;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    // Helper methods
    public boolean isFileImport() {
        return connectionType == BankConnectionType.FILE_IMPORT;
    }

    public boolean isOpenBanking() {
        return connectionType == BankConnectionType.SALT_EDGE;
    }

    public boolean isSaltEdgeActive() {
        return "active".equalsIgnoreCase(saltEdgeStatus);
    }

    public boolean isConsentValid() {
        return saltEdgeConsentExpiresAt != null &&
               saltEdgeConsentExpiresAt.isAfter(OffsetDateTime.now());
    }
}
