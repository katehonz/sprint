package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateExchangeRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCurrencyInput;
import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;
import bg.spacbg.sp_ac_bg.model.entity.ExchangeRateEntity;
import bg.spacbg.sp_ac_bg.service.CurrencyService;
import bg.spacbg.sp_ac_bg.service.ExchangeRateService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    public CurrencyController(CurrencyService currencyService, ExchangeRateService exchangeRateService) {
        this.currencyService = currencyService;
        this.exchangeRateService = exchangeRateService;
    }

    // Currency Queries
    @QueryMapping
    public List<CurrencyEntity> currencies() {
        return currencyService.findAll();
    }

    @QueryMapping
    public CurrencyEntity currency(@Argument Integer id) {
        return currencyService.findById(id).orElse(null);
    }

    @QueryMapping
    public CurrencyEntity currencyByCode(@Argument String code) {
        return currencyService.findByCode(code).orElse(null);
    }

    @QueryMapping
    public CurrencyEntity baseCurrency() {
        return currencyService.findBaseCurrency().orElse(null);
    }

    // Currency Mutations
    @MutationMapping
    public CurrencyEntity createCurrency(@Argument CreateCurrencyInput input) {
        return currencyService.create(input);
    }

    @MutationMapping
    public CurrencyEntity updateCurrency(@Argument Integer id, @Argument UpdateCurrencyInput input) {
        return currencyService.update(id, input);
    }

    // Exchange Rate Queries
    @QueryMapping
    public List<ExchangeRateEntity> exchangeRates(@Argument Integer fromCurrencyId, @Argument Integer toCurrencyId) {
        return exchangeRateService.findByFromAndTo(fromCurrencyId, toCurrencyId);
    }

    @QueryMapping
    public List<ExchangeRateEntity> allExchangeRates(@Argument String baseCurrency) {
        if (baseCurrency != null && !baseCurrency.isEmpty()) {
            return exchangeRateService.findByBaseCurrency(baseCurrency);
        }
        return exchangeRateService.findAll();
    }

    @QueryMapping
    public ExchangeRateEntity exchangeRate(@Argument String fromCode, @Argument String toCode, @Argument LocalDate date) {
        return exchangeRateService.findByCodesAndDate(fromCode, toCode, date).orElse(null);
    }

    @QueryMapping
    public ExchangeRateEntity latestExchangeRate(@Argument String fromCode, @Argument String toCode) {
        return exchangeRateService.findLatest(fromCode, toCode).orElse(null);
    }

    // Exchange Rate Mutations
    @MutationMapping
    public ExchangeRateEntity createExchangeRate(@Argument CreateExchangeRateInput input) {
        return exchangeRateService.create(input);
    }

    @MutationMapping
    public List<ExchangeRateEntity> fetchEcbRates(@Argument LocalDate date) {
        return exchangeRateService.fetchEcbRates(date);
    }
}
