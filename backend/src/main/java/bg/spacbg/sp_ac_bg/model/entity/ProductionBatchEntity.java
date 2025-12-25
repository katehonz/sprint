package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.ProductionBatchStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "production_batches", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_id", "batch_number"})
})
public class ProductionBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_card_id", nullable = false)
    private TechnologyCardEntity technologyCard;

    @Column(nullable = false, length = 50)
    private String batchNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQuantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualQuantity;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductionBatchStatus status = ProductionBatchStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @OneToMany(mappedBy = "productionBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionBatchStageEntity> stages;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
