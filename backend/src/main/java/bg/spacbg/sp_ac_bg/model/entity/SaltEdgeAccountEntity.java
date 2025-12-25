package bg.spacbg.sp_ac_bg.model.entity;

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
@Table(name = "salt_edge_accounts")
public class SaltEdgeAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "salt_edge_connection_id", nullable = false)
    private String saltEdgeConnectionId;

    @Column(name = "salt_edge_account_id", nullable = false, unique = true)
    private String saltEdgeAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_profile_id")
    private BankProfileEntity bankProfile;

    @Column(nullable = false)
    private String name;

    private String nature; // checking, savings, credit_card, etc.

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "available_amount", precision = 19, scale = 4)
    private BigDecimal availableAmount;

    @Column(length = 50)
    private String iban;

    @Column(length = 20)
    private String swift;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "is_mapped", nullable = false)
    private Boolean isMapped = false;

    @Column(name = "extra_data", columnDefinition = "jsonb")
    private String extraData;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
