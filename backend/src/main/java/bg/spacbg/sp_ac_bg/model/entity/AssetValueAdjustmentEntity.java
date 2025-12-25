package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.AssetAdjustmentType;
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
@Table(name = "asset_value_adjustments")
public class AssetValueAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_asset_id", nullable = false)
    private FixedAssetEntity fixedAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private LocalDate adjustmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetAdjustmentType adjustmentType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal adjustmentAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingValueBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingValueAfter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxValueBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxValueAfter;

    @Column(precision = 19, scale = 4)
    private BigDecimal newAccountingDepreciationRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal newTaxDepreciationRate;

    private Integer newAccountingUsefulLife;

    private Integer newTaxUsefulLife;

    @Column(nullable = false)
    private String reason;

    private String documentNumber;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @Column(nullable = false)
    private boolean isPosted;

    private OffsetDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private UserEntity postedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
