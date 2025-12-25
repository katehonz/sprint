package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateExchangeRateInput;
import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;
import bg.spacbg.sp_ac_bg.model.entity.ExchangeRateEntity;
import bg.spacbg.sp_ac_bg.model.enums.RateSource;
import bg.spacbg.sp_ac_bg.repository.CurrencyRepository;
import bg.spacbg.sp_ac_bg.repository.ExchangeRateRepository;
import bg.spacbg.sp_ac_bg.service.ExchangeRateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final String ECB_DAILY_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final RestTemplate restTemplate;

    public ExchangeRateServiceImpl(
            ExchangeRateRepository exchangeRateRepository,
            CurrencyRepository currencyRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyRepository = currencyRepository;
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateEntity> findAll() {
        return exchangeRateRepository.findAllByOrderByValidDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateEntity> findByBaseCurrency(String baseCurrency) {
        return exchangeRateRepository.findByFromCurrencyCodeOrderByValidDateDesc(baseCurrency.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateEntity> findByFromAndTo(Integer fromCurrencyId, Integer toCurrencyId) {
        return exchangeRateRepository.findByFromCurrencyIdAndToCurrencyIdOrderByValidDateDesc(
                fromCurrencyId, toCurrencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRateEntity> findByCodesAndDate(String fromCode, String toCode, LocalDate date) {
        return exchangeRateRepository.findByCodesAndDate(fromCode.toUpperCase(), toCode.toUpperCase(), date);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExchangeRateEntity> findLatest(String fromCode, String toCode) {
        return exchangeRateRepository.findLatestByCodesActive(fromCode.toUpperCase(), toCode.toUpperCase());
    }

    @Override
    public ExchangeRateEntity create(CreateExchangeRateInput input) {
        CurrencyEntity fromCurrency = currencyRepository.findByCode(input.getFromCurrencyCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Валутата не е намерена: " + input.getFromCurrencyCode()));

        CurrencyEntity toCurrency = currencyRepository.findByCode(input.getToCurrencyCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Валутата не е намерена: " + input.getToCurrencyCode()));

        if (exchangeRateRepository.existsByFromCurrencyIdAndToCurrencyIdAndValidDate(
                fromCurrency.getId(), toCurrency.getId(), input.getValidDate())) {
            throw new IllegalArgumentException("Курс за тази дата вече съществува");
        }

        ExchangeRateEntity rate = new ExchangeRateEntity();
        rate.setFromCurrency(fromCurrency);
        rate.setToCurrency(toCurrency);
        rate.setRate(input.getRate());
        rate.setReverseRate(BigDecimal.ONE.divide(input.getRate(), 6, RoundingMode.HALF_UP));
        rate.setValidDate(input.getValidDate());
        rate.setRateSource(input.getRateSource() != null ? input.getRateSource() : RateSource.MANUAL);
        rate.setNotes(input.getNotes());
        rate.setActive(true);

        return exchangeRateRepository.save(rate);
    }

    @Override
    public List<ExchangeRateEntity> fetchEcbRates(LocalDate date) {
        List<ExchangeRateEntity> rates = new ArrayList<>();

        try {
            String xml = restTemplate.getForObject(ECB_DAILY_URL, String.class);
            if (xml == null) {
                throw new RuntimeException("Не може да се получат курсове от ЕЦБ");
            }

            CurrencyEntity eurCurrency = currencyRepository.findByCode("EUR")
                    .orElseThrow(() -> new IllegalArgumentException("EUR валутата не е конфигурирана"));

            // Извличаме датата от XML: <Cube time='2025-11-28'>
            LocalDate rateDate = date;
            if (rateDate == null) {
                String timeAttr = extractAttribute(xml, "time");
                if (timeAttr != null) {
                    rateDate = LocalDate.parse(timeAttr);
                } else {
                    rateDate = LocalDate.now();
                }
            }

            // Парсване на XML - разделяме по <Cube
            // Формат: <Cube currency='USD' rate='1.0876'/>
            String[] cubes = xml.split("<Cube ");
            for (String cube : cubes) {
                if (cube.contains("currency=") && cube.contains("rate=")) {
                    String currency = extractAttribute(cube, "currency");
                    String rateStr = extractAttribute(cube, "rate");

                    if (currency != null && rateStr != null) {
                        Optional<CurrencyEntity> targetCurrency = currencyRepository.findByCode(currency);
                        if (targetCurrency.isPresent()) {
                            BigDecimal rateValue = new BigDecimal(rateStr);

                            // Проверка дали вече съществува
                            if (!exchangeRateRepository.existsByFromCurrencyIdAndToCurrencyIdAndValidDate(
                                    eurCurrency.getId(), targetCurrency.get().getId(), rateDate)) {

                                ExchangeRateEntity rate = new ExchangeRateEntity();
                                rate.setFromCurrency(eurCurrency);
                                rate.setToCurrency(targetCurrency.get());
                                rate.setRate(rateValue);
                                rate.setReverseRate(BigDecimal.ONE.divide(rateValue, 6, RoundingMode.HALF_UP));
                                rate.setValidDate(rateDate);
                                rate.setRateSource(RateSource.ECB);
                                rate.setActive(true);

                                rates.add(exchangeRateRepository.save(rate));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Грешка при получаване на курсове от ЕЦБ: " + e.getMessage(), e);
        }

        return rates;
    }

    private String extractAttribute(String line, String attribute) {
        // Опитваме с двойни кавички
        String search = attribute + "=\"";
        int start = line.indexOf(search);
        char quote = '"';

        // Ако не намерим, опитваме с единични кавички
        if (start < 0) {
            search = attribute + "='";
            start = line.indexOf(search);
            quote = '\'';
        }

        if (start < 0) return null;
        start += search.length();
        int end = line.indexOf(String.valueOf(quote), start);
        if (end < 0) return null;
        return line.substring(start, end);
    }
}
