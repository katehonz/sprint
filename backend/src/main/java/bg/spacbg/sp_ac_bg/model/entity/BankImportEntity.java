package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.BankImportStatus;
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
@Table(name = "bank_imports")
public class BankImportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_profile_id", nullable = false)
    private BankProfileEntity bankProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String importFormat;

    @Column(nullable = false)
    private OffsetDateTime importedAt;

    @Column(nullable = false)
    private Integer transactionsCount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCredit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebit;

    @Column(nullable = false)
    private Integer createdJournalEntries;

    @Column(columnDefinition = "jsonb") // Storing JSON array as String for simplicity, can be mapped to List<Integer> with custom converter
    private String journalEntryIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankImportStatus status;

    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
