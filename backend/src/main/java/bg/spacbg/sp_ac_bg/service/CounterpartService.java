package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.entity.CounterpartEntity;

import java.util.List;
import java.util.Optional;

public interface CounterpartService {
    List<CounterpartEntity> findByCompanyId(Integer companyId);
    List<CounterpartEntity> search(Integer companyId, String query);
    Optional<CounterpartEntity> findById(Integer id);
    CounterpartEntity create(CreateCounterpartInput input);
    CounterpartEntity update(Integer id, UpdateCounterpartInput input);
    boolean delete(Integer id);
}
