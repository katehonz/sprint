package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuditLogFilter {
    private Integer companyId;
    private Integer userId;
    private String action;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String search;
    private Integer offset;
    private Integer limit;
}
