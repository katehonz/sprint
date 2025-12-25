package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.DeclarationType;
import bg.spacbg.sp_ac_bg.model.enums.DeclarationStatus;
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
@Table(name = "intrastat_declarations")
public class IntrastatDeclarationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeclarationType declarationType;

    @Column(nullable = false)
    private String referencePeriod;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    private String declarationNumber;

    @Column(nullable = false)
    private String declarantEik;

    @Column(nullable = false)
    private String declarantName;

    @Column(nullable = false)
    private String contactPerson;

    @Column(nullable = false)
    private String contactPhone;

    @Column(nullable = false)
    private String contactEmail;

    @Column(nullable = false)
    private Integer totalItems;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalStatisticalValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalInvoiceValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeclarationStatus status;

    private OffsetDateTime submissionDate;

    private String xmlFilePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntrastatDeclarationItemEntity> items;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
