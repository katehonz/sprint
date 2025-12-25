package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
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
@Table(name = "vat_rates")
public class VatRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VatDirection vatDirection;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
