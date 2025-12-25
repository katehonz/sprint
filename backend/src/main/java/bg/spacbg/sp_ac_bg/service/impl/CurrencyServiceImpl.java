package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;
import bg.spacbg.sp_ac_bg.repository.CurrencyRepository;
import bg.spacbg.sp_ac_bg.service.CurrencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyServiceImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyEntity> findAll() {
        return currencyRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrencyEntity> findById(Integer id) {
        return currencyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrencyEntity> findByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrencyEntity> findBaseCurrency() {
        return currencyRepository.findByIsBaseCurrencyTrue();
    }

    @Override
    public CurrencyEntity create(CreateCurrencyInput input) {
        String code = input.getCode().toUpperCase();
        if (currencyRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Валута с код " + code + " вече съществува");
        }

        CurrencyEntity currency = new CurrencyEntity();
        currency.setCode(code);
        currency.setName(input.getName());
        currency.setNameBg(input.getNameBg());
        currency.setSymbol(input.getSymbol());
        currency.setDecimalPlaces(input.getDecimalPlaces() != null ? input.getDecimalPlaces() : 2);
        currency.setActive(true);

        // Ако е базова валута, премахни флага от другите
        if (Boolean.TRUE.equals(input.getIsBaseCurrency())) {
            currencyRepository.findByIsBaseCurrencyTrue().ifPresent(existing -> {
                existing.setBaseCurrency(false);
                currencyRepository.save(existing);
            });
            currency.setBaseCurrency(true);
        } else {
            currency.setBaseCurrency(false);
        }

        return currencyRepository.save(currency);
    }

    @Override
    public CurrencyEntity update(Integer id, UpdateCurrencyInput input) {
        CurrencyEntity currency = currencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Валутата не е намерена: " + id));

        if (input.getName() != null) currency.setName(input.getName());
        if (input.getNameBg() != null) currency.setNameBg(input.getNameBg());
        if (input.getSymbol() != null) currency.setSymbol(input.getSymbol());
        if (input.getDecimalPlaces() != null) currency.setDecimalPlaces(input.getDecimalPlaces());
        if (input.getIsActive() != null) currency.setActive(input.getIsActive());

        return currencyRepository.save(currency);
    }
}
