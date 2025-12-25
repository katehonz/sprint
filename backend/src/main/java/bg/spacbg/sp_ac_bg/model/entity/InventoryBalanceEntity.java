package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_balances")
public class InventoryBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAverageCost;

    private LocalDate lastMovementDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_movement_id")
    private InventoryMovementEntity lastMovement;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
