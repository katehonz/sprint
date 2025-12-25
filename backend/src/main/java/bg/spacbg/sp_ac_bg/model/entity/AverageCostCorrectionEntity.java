package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "average_cost_corrections")
public class AverageCostCorrectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggering_movement_id", nullable = false)
    private InventoryMovementEntity triggeringMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_movement_id", nullable = false)
    private InventoryMovementEntity affectedMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correction_journal_entry_id")
    private JournalEntryEntity correctionJournalEntry;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal oldAverageCost;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal newAverageCost;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal correctionAmount;

    @Column(nullable = false)
    private boolean isApplied;

    private OffsetDateTime appliedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
