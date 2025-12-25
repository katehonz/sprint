package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "currencies")
public class CurrencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nameBg;

    private String symbol;

    @Column(nullable = false)
    private Integer decimalPlaces;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isBaseCurrency;

    private String bnbCode;

    @OneToMany(mappedBy = "fromCurrency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExchangeRateEntity> fromExchangeRates; // Placeholder

    @OneToMany(mappedBy = "toCurrency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExchangeRateEntity> toExchangeRates; // Placeholder

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
