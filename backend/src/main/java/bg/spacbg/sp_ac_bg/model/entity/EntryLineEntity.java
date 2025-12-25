package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entry_lines")
public class EntryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal debitAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal creditAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterpart_id")
    private CounterpartEntity counterpart;

    private String currencyCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal currencyAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id")
    private VatRateEntity vatRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal quantity;

    private String unitOfMeasureCode;

    private String description;

    @Column(nullable = false)
    private Integer lineOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    // Alias getters for GraphQL schema compatibility
    public Integer getAccountId() {
        return account != null ? account.getId() : null;
    }

    public Integer getCounterpartId() {
        return counterpart != null ? counterpart.getId() : null;
    }

    public Integer getJournalEntryId() {
        return journalEntry != null ? journalEntry.getId() : null;
    }
}
