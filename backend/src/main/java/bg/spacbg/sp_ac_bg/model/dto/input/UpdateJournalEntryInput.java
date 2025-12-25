package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateJournalEntryInput {
    private LocalDate documentDate;
    private LocalDate vatDate;
    private LocalDate accountingDate;
    private String documentNumber;
    private String description;
    private List<CreateEntryLineInput> lines;
    private String documentType;
    private String vatDocumentType;
}
