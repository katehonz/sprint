package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.time.LocalDate;

/**
 * Входни параметри за проверка на корекции на СПЦ
 */
@Data
public class CheckCorrectionsInput {
    private Integer companyId;
    private Integer accountId;
    private LocalDate newEntryDate;
}
