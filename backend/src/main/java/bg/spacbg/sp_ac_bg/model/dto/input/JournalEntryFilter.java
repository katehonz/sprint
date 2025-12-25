package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JournalEntryFilter {
    private Integer companyId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate vatFromDate;
    private LocalDate vatToDate;
    private Integer accountId;
    private Boolean isPosted;
    private String documentNumber;
    private String search;
    private String vatPurchaseOperation;
    private String vatSalesOperation;
    private Integer offset;
    private Integer limit;
}
