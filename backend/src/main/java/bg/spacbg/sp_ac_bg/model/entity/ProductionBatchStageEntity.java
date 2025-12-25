package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStageStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "production_batch_stages")
public class ProductionBatchStageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_batch_id", nullable = false)
    private ProductionBatchEntity productionBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_card_stage_id", nullable = false)
    private TechnologyCardStageEntity technologyCardStage;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQuantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualQuantity;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductionBatchStageStatus status = ProductionBatchStageStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
