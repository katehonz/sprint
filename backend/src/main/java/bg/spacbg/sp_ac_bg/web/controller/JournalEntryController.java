package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.JournalEntryFilter;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.output.JournalEntriesPage;
import bg.spacbg.sp_ac_bg.model.entity.CounterpartEntity;
import bg.spacbg.sp_ac_bg.model.entity.EntryLineEntity;
import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;
import bg.spacbg.sp_ac_bg.service.JournalEntryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @QueryMapping
    public List<JournalEntryEntity> journalEntries(@Argument JournalEntryFilter filter) {
        return journalEntryService.findByFilter(filter);
    }

    @QueryMapping
    public JournalEntriesPage journalEntriesPaged(@Argument JournalEntryFilter filter) {
        return journalEntryService.findByFilterPaged(filter);
    }

    @QueryMapping
    public JournalEntryEntity journalEntry(@Argument Integer id) {
        return journalEntryService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<JournalEntryEntity> unpostedEntries(@Argument Integer companyId) {
        return journalEntryService.findUnposted(companyId);
    }

    @MutationMapping
    public JournalEntryEntity createJournalEntry(@Argument CreateJournalEntryInput input) {
        Integer userId = getCurrentUserId();
        return journalEntryService.create(input, userId);
    }

    @MutationMapping
    public JournalEntryEntity updateJournalEntry(@Argument Integer id, @Argument UpdateJournalEntryInput input) {
        return journalEntryService.update(id, input);
    }

    @MutationMapping
    public Boolean deleteJournalEntry(@Argument Integer id) {
        return journalEntryService.delete(id);
    }

    @MutationMapping
    public JournalEntryEntity postJournalEntry(@Argument Integer id) {
        Integer userId = getCurrentUserId();
        return journalEntryService.post(id, userId);
    }

    @MutationMapping
    public JournalEntryEntity unpostJournalEntry(@Argument Integer id) {
        return journalEntryService.unpost(id);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from authentication
        return 1; // Placeholder - should be extracted from JWT token
    }
}
