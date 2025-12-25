package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String entryNumber;

    @Column(nullable = false)
    private LocalDate documentDate;

    private LocalDate vatDate;

    @Column(nullable = false)
    private LocalDate accountingDate;

    private String documentNumber;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalVatAmount;

    @Column(nullable = false)
    private boolean isPosted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private UserEntity postedBy;

    private OffsetDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterpart_id")
    private CounterpartEntity counterpart;

    private String documentType;

    private String vatDocumentType;

    private String vatPurchaseOperation;

    private String vatSalesOperation;

    private String vatAdditionalOperation;

    private String vatAdditionalData;

    @Column(precision = 19, scale = 4)
    private BigDecimal vatRate;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntryLineEntity> entryLines;

    // Alias for GraphQL schema compatibility (schema uses 'lines', entity uses 'entryLines')
    public List<EntryLineEntity> getLines() {
        return entryLines;
    }

    // Alias for GraphQL schema compatibility
    public Integer getCompanyId() {
        return company != null ? company.getId() : null;
    }

    // Alias for GraphQL schema compatibility
    public Integer getCounterpartId() {
        return counterpart != null ? counterpart.getId() : null;
    }

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
