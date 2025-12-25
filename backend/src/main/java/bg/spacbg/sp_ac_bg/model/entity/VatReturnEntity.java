package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.VatReturnStatus;
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
@Table(name = "vat_returns")
public class VatReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false)
    private Integer periodMonth;

    @Column(nullable = false)
    private LocalDate periodFrom;

    @Column(nullable = false)
    private LocalDate periodTo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal outputVatAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal inputVatAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vatToPay;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vatToRefund;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal effectiveVatToPay;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal vatForDeduction;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal vatRefundArt92;

    @Column(name = "base_amount20", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount20;

    @Column(name = "vat_amount20", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount20;

    @Column(name = "base_amount9", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount9;

    @Column(name = "vat_amount9", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount9;

    @Column(name = "base_amount0", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount0;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal exemptAmount;

    @Column(name = "sales_base20", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBase20;

    @Column(name = "sales_vat20", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesVat20;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBaseVop;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesVatVop;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesVatPersonalUse;

    @Column(name = "sales_base9", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBase9;

    @Column(name = "sales_vat9", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesVat9;

    @Column(name = "sales_base0_art3", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBase0Art3;

    @Column(name = "sales_base0_vod", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBase0Vod;

    @Column(name = "sales_base0_export", nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBase0Export;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBaseArt21;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBaseArt69;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salesBaseExempt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseBaseNoCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseBaseFullCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseVatFullCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseBasePartialCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseVatPartialCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseVatAnnualAdjustment;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal creditCoefficient;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeductibleVat;

    @Column(nullable = false)
    private Integer salesDocumentCount;

    @Column(nullable = false)
    private Integer purchaseDocumentCount;

    private String submittedByPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VatReturnStatus status;

    private OffsetDateTime submittedAt;

    private OffsetDateTime calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private UserEntity submittedBy;

    @Column(nullable = false)
    private LocalDate dueDate;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
