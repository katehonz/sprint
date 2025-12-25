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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fixed_assets")
public class FixedAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String inventoryNumber;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FixedAssetCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal acquisitionCost;

    @Column(nullable = false)
    private LocalDate acquisitionDate;

    private String documentNumber;

    private LocalDate documentDate;

    private LocalDate putIntoServiceDate;

    @Column(nullable = false)
    private Integer accountingUsefulLife;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingDepreciationRate;

    @Column(nullable = false)
    private String accountingDepreciationMethod;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingSalvageValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingAccumulatedDepreciation;

    private Integer taxUsefulLife;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxDepreciationRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAccumulatedDepreciation;

    @Column(nullable = false)
    private boolean isNewFirstTimeInvestment;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal accountingBookValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxBookValue;

    @Column(nullable = false)
    private String status;

    private LocalDate disposalDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal disposalAmount;

    private String location;

    private String responsiblePerson;

    private String serialNumber;

    private String manufacturer;

    private String model;

    private String notes;

    @OneToMany(mappedBy = "fixedAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepreciationJournalEntity> depreciationJournal;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
