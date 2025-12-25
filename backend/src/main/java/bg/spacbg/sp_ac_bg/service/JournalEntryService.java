package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.JournalEntryFilter;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateJournalEntryInput;
import bg.spacbg.sp_ac_bg.model.dto.output.JournalEntriesPage;
import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;

import java.util.List;
import java.util.Optional;

public interface JournalEntryService {
    List<JournalEntryEntity> findByFilter(JournalEntryFilter filter);
    JournalEntriesPage findByFilterPaged(JournalEntryFilter filter);
    List<JournalEntryEntity> findUnposted(Integer companyId);
    Optional<JournalEntryEntity> findById(Integer id);
    JournalEntryEntity create(CreateJournalEntryInput input, Integer userId);
    JournalEntryEntity update(Integer id, UpdateJournalEntryInput input);
    boolean delete(Integer id);
    JournalEntryEntity post(Integer id, Integer userId);
    JournalEntryEntity unpost(Integer id);
    String generateEntryNumber(Integer companyId);
}
