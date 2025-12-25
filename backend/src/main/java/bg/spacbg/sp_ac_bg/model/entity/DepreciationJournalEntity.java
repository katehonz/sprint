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
@Table(name = "depreciation_journal")
public class DepreciationJournalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_asset_id", nullable = false)
    private FixedAssetEntity fixedAsset;

    @Column(nullable = false)
    private LocalDate period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingDepreciationAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingBookValueBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingBookValueAfter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxDepreciationAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxBookValueBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxBookValueAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @Column(nullable = false)
    private boolean isPosted;

    private OffsetDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private UserEntity postedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
