package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateExchangeRateInput;
import bg.spacbg.sp_ac_bg.model.entity.ExchangeRateEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateService {
    List<ExchangeRateEntity> findAll();
    List<ExchangeRateEntity> findByBaseCurrency(String baseCurrency);
    List<ExchangeRateEntity> findByFromAndTo(Integer fromCurrencyId, Integer toCurrencyId);
    Optional<ExchangeRateEntity> findByCodesAndDate(String fromCode, String toCode, LocalDate date);
    Optional<ExchangeRateEntity> findLatest(String fromCode, String toCode);
    ExchangeRateEntity create(CreateExchangeRateInput input);
    List<ExchangeRateEntity> fetchEcbRates(LocalDate date);
}
