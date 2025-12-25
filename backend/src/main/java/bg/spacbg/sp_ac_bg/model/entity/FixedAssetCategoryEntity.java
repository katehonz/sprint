package bg.spacbg.sp_ac_bg.model.entity;

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
@Table(name = "fixed_asset_categories")
public class FixedAssetCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer taxCategory;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxTaxDepreciationRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal defaultAccountingDepreciationRate;

    private Integer minUsefulLife;

    private Integer maxUsefulLife;

    @Column(nullable = false)
    private String assetAccountCode;

    @Column(nullable = false)
    private String depreciationAccountCode;

    @Column(nullable = false)
    private String expenseAccountCode;

    private String improvementDebitAccountCode;

    private String improvementCreditAccountCode;

    private String impairmentDebitAccountCode;

    private String impairmentCreditAccountCode;

    private String disposalDebitAccountCode;

    private String disposalCreditAccountCode;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FixedAssetEntity> fixedAssets;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
