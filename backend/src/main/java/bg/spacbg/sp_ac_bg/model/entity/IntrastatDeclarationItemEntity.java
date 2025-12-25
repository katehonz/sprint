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
@Table(name = "intrastat_declaration_items")
public class IntrastatDeclarationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private IntrastatDeclarationEntity declaration;

    @Column(nullable = false)
    private Integer itemNumber;

    @Column(nullable = false)
    private String cnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomenclature_id")
    private IntrastatNomenclatureEntity nomenclature;

    @Column(nullable = false)
    private String countryOfOrigin;

    @Column(nullable = false)
    private String countryOfConsignment;

    @Column(nullable = false)
    private String transactionNatureCode;

    @Column(nullable = false)
    private Integer transportMode;

    @Column(nullable = false)
    private String deliveryTerms;

    private String statisticalProcedure;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal netMassKg;

    @Column(precision = 19, scale = 4)
    private BigDecimal supplementaryUnit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal invoiceValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal statisticalValue;

    @Column(nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private String description;

    private String regionCode;

    private String portCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_line_id")
    private EntryLineEntity entryLine;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
