package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity for storing scanned/recognized invoices before they are processed into journal entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "scanned_invoices")
public class ScannedInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    // Invoice direction: PURCHASE or SALE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceDirection direction;

    // Processing status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    // Vendor (seller) information
    @Column(length = 500)
    private String vendorName;

    @Column(length = 50)
    private String vendorVatNumber;

    @Column(length = 1000)
    private String vendorAddress;

    // Customer (buyer) information
    @Column(length = 500)
    private String customerName;

    @Column(length = 50)
    private String customerVatNumber;

    @Column(length = 1000)
    private String customerAddress;

    // Invoice details
    @Column(length = 100)
    private String invoiceNumber;

    private LocalDate invoiceDate;

    private LocalDate dueDate;

    // Financial amounts
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalTax;

    @Column(precision = 15, scale = 2)
    private BigDecimal invoiceTotal;

    // VIES validation status
    @Enumerated(EnumType.STRING)
    private ViesStatus viesStatus;

    @Column(length = 500)
    private String viesValidationMessage;

    @Column(length = 500)
    private String viesCompanyName;

    @Column(length = 1000)
    private String viesCompanyAddress;

    private OffsetDateTime viesValidatedAt;

    // Selected accounts (user-editable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_account_id")
    private AccountEntity counterpartyAccount;  // 401 for purchase, 411 for sale

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_account_id")
    private AccountEntity vatAccount;  // 4531 for purchase, 4532 for sale

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_revenue_account_id")
    private AccountEntity expenseRevenueAccount;  // Expense for purchase, Revenue for sale

    // Notes and manual review
    private boolean requiresManualReview;

    @Column(length = 500)
    private String manualReviewReason;

    @Column(length = 2000)
    private String notes;

    // Reference to created journal entry (after processing)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    // Original file reference (optional)
    @Column(length = 500)
    private String originalFileName;

    // User who scanned/created
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public enum InvoiceDirection {
        PURCHASE,  // We are buying (vendor sends invoice to us)
        SALE       // We are selling (we send invoice to customer)
    }

    public enum ProcessingStatus {
        PENDING,      // Just scanned, awaiting review
        VALIDATED,    // VIES validated, ready for processing
        REJECTED,     // Rejected by user or validation failed
        PROCESSED     // Journal entry created
    }

    public enum ViesStatus {
        PENDING,        // Not yet validated
        VALID,          // VAT number is valid
        INVALID,        // VAT number is invalid
        NOT_APPLICABLE, // Non-EU or domestic
        ERROR           // Validation service error
    }
}
