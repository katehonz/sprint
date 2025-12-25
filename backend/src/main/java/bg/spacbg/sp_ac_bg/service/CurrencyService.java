package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;

import java.util.List;
import java.util.Optional;

public interface CurrencyService {
    List<CurrencyEntity> findAll();
    Optional<CurrencyEntity> findById(Integer id);
    Optional<CurrencyEntity> findByCode(String code);
    Optional<CurrencyEntity> findBaseCurrency();
    CurrencyEntity create(CreateCurrencyInput input);
    CurrencyEntity update(Integer id, UpdateCurrencyInput input);
}
