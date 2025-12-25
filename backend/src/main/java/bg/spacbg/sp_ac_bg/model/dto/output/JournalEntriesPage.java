package bg.spacbg.sp_ac_bg.model.dto.output;

import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntriesPage {
    private List<JournalEntryEntity> entries;
    private Long totalCount;
    private Boolean hasMore;
}
