package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.FlowDirection;
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
@Table(name = "intrastat_account_mapping")
public class IntrastatAccountMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomenclature_id", nullable = false)
    private IntrastatNomenclatureEntity nomenclature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowDirection flowDirection;

    @Column(nullable = false)
    private String transactionNatureCode;

    @Column(nullable = false)
    private boolean isQuantityTracked;

    private String defaultCountryCode;

    private Integer defaultTransportMode;

    @Column(nullable = false)
    private boolean isOptional;

    @Column(precision = 19, scale = 4)
    private BigDecimal minThresholdBgn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
