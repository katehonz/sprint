package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJournalEntryInput {
    private String entryNumber;
    private LocalDate documentDate;
    private LocalDate vatDate;
    private LocalDate accountingDate;
    private String documentNumber;
    private String description;
    private Integer companyId;
    private Integer counterpartId;
    private BigDecimal totalAmount;
    private BigDecimal totalVatAmount;
    private List<CreateEntryLineInput> lines;
    private String documentType;
    private String vatDocumentType;
    private String vatPurchaseOperation;
    private String vatSalesOperation;
    private Integer scannedInvoiceId;
}
