package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.RateSource;
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
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_id", nullable = false)
    private CurrencyEntity fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_id", nullable = false)
    private CurrencyEntity toCurrency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reverseRate;

    @Column(nullable = false)
    private LocalDate validDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RateSource rateSource;

    private String bnbRateId;

    @Column(nullable = false)
    private boolean isActive = true;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
