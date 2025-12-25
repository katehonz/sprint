package bg.spacbg.sp_ac_bg.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognizedInvoice {
    // Vendor (seller) information
    private String vendorName;
    private String vendorVatNumber;      // VAT/Tax ID of the vendor
    private String vendorAddress;

    // Customer (buyer) information
    private String customerName;
    private String customerVatNumber;    // VAT/Tax ID of the customer
    private String customerAddress;

    // Invoice details
    private String invoiceId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Financial amounts
    private BigDecimal subtotal;         // Net amount (without VAT)
    private BigDecimal totalTax;         // VAT amount
    private BigDecimal invoiceTotal;     // Gross total

    // Auto-determined invoice type based on our company's VAT number
    private InvoiceDirection direction;  // PURCHASE or SALE

    // Validation status
    private ValidationStatus validationStatus;
    private String viesValidationMessage;

    // Suggested accounts based on automation settings
    private SuggestedAccounts suggestedAccounts;

    // Whether manual review is required (e.g., no VAT number found)
    private boolean requiresManualReview;
    private String manualReviewReason;

    public enum InvoiceDirection {
        PURCHASE,  // We are the customer (buying)
        SALE,      // We are the vendor (selling)
        UNKNOWN    // Could not determine
    }

    public enum ValidationStatus {
        PENDING,           // Not yet validated
        VALID,             // VAT number validated via VIES
        INVALID,           // VAT number invalid
        NOT_APPLICABLE,    // Non-EU or no VAT number
        MANUAL_REVIEW      // Requires manual review
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedAccounts {
        private AccountInfo counterpartyAccount;  // 401 for purchase, 411 for sale
        private AccountInfo vatAccount;           // 4531 for purchase, 4532 for sale
        private AccountInfo expenseOrRevenueAccount; // Expense account for purchase, Revenue for sale
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private Integer id;
        private String code;
        private String name;
    }
}
