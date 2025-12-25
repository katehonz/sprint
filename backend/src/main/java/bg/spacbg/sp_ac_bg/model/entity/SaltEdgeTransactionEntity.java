package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "salt_edge_transactions")
public class SaltEdgeTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "salt_edge_account_id", nullable = false)
    private String saltEdgeAccountId;

    @Column(name = "salt_edge_transaction_id", nullable = false, unique = true)
    private String saltEdgeTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_profile_id")
    private BankProfileEntity bankProfile;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String mode; // normal, fee, transfer

    @Column(nullable = false, length = 50)
    private String status; // posted, pending

    private Boolean duplicated = false;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @Column(name = "extra_data", columnDefinition = "jsonb")
    private String extraData;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    // Helper methods
    public boolean isCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
}
