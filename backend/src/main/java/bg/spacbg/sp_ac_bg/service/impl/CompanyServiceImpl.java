package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCompanyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCompanyInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;
import bg.spacbg.sp_ac_bg.model.enums.RateProvider;
import bg.spacbg.sp_ac_bg.repository.AccountRepository;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.repository.CurrencyRepository;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountRepository accountRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              CurrencyRepository currencyRepository,
                              AccountRepository accountRepository) {
        this.companyRepository = companyRepository;
        this.currencyRepository = currencyRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyEntity> findAll() {
        return companyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyEntity> findById(Integer id) {
        return companyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyEntity> findByEik(String eik) {
        return companyRepository.findByEik(eik);
    }

    @Override
    public CompanyEntity create(CreateCompanyInput input) {
        if (companyRepository.existsByEik(input.getEik())) {
            throw new IllegalArgumentException("Компания с ЕИК " + input.getEik() + " вече съществува");
        }

        CompanyEntity company = new CompanyEntity();
        company.setName(input.getName());
        company.setEik(input.getEik());
        company.setVatNumber(input.getVatNumber());
        company.setAddress(input.getAddress());
        company.setCity(input.getCity());
        company.setCountry(input.getCountry() != null ? input.getCountry() : "България");
        company.setPhone(input.getPhone());
        company.setEmail(input.getEmail());
        company.setContactPerson(input.getContactPerson());
        company.setManagerName(input.getManagerName());
        company.setManagerEgn(input.getManagerEgn());
        company.setAuthorizedPerson(input.getAuthorizedPerson());
        company.setAuthorizedPersonEgn(input.getAuthorizedPersonEgn());
        company.setNapOffice(input.getNapOffice());
        company.setAzureFormRecognizerEndpoint(input.getAzureFormRecognizerEndpoint());
        company.setAzureFormRecognizerKey(input.getAzureFormRecognizerKey());
        company.setActive(true);
        company.setEnableViesValidation(input.getEnableViesValidation() != null ? input.getEnableViesValidation() : false);
        company.setEnableAiMapping(input.getEnableAiMapping() != null ? input.getEnableAiMapping() : false);
        company.setAutoValidateOnImport(input.getAutoValidateOnImport() != null ? input.getAutoValidateOnImport() : false);
        company.setPreferredRateProvider(input.getPreferredRateProvider() != null ? input.getPreferredRateProvider() : RateProvider.ECB);

        // Set base currency - default to EUR if not specified (Bulgaria joins eurozone 01.01.2026)
        if (input.getBaseCurrencyId() != null) {
            CurrencyEntity currency = currencyRepository.findById(input.getBaseCurrencyId())
                    .orElseThrow(() -> new IllegalArgumentException("Валутата не е намерена: " + input.getBaseCurrencyId()));
            company.setBaseCurrency(currency);
        } else {
            // Default to EUR (eurozone 2026)
            currencyRepository.findByCode("EUR").ifPresent(company::setBaseCurrency);
        }

        return companyRepository.save(company);
    }

    @Override
    public CompanyEntity update(Integer id, UpdateCompanyInput input) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + id));

        if (input.getName() != null) company.setName(input.getName());
        if (input.getEik() != null) {
            if (!company.getEik().equals(input.getEik()) && companyRepository.existsByEik(input.getEik())) {
                throw new IllegalArgumentException("Компания с ЕИК " + input.getEik() + " вече съществува");
            }
            company.setEik(input.getEik());
        }
        if (input.getVatNumber() != null) company.setVatNumber(input.getVatNumber());
        if (input.getAddress() != null) company.setAddress(input.getAddress());
        if (input.getCity() != null) company.setCity(input.getCity());
        if (input.getCountry() != null) company.setCountry(input.getCountry());
        if (input.getPhone() != null) company.setPhone(input.getPhone());
        if (input.getEmail() != null) company.setEmail(input.getEmail());
        if (input.getContactPerson() != null) company.setContactPerson(input.getContactPerson());
        if (input.getManagerName() != null) company.setManagerName(input.getManagerName());
        if (input.getManagerEgn() != null) company.setManagerEgn(input.getManagerEgn());
        if (input.getAuthorizedPerson() != null) company.setAuthorizedPerson(input.getAuthorizedPerson());
        if (input.getAuthorizedPersonEgn() != null) company.setAuthorizedPersonEgn(input.getAuthorizedPersonEgn());
        if (input.getNapOffice() != null) company.setNapOffice(input.getNapOffice());
        if (input.getAzureFormRecognizerEndpoint() != null) company.setAzureFormRecognizerEndpoint(input.getAzureFormRecognizerEndpoint());
        if (input.getAzureFormRecognizerKey() != null) company.setAzureFormRecognizerKey(input.getAzureFormRecognizerKey());
        if (input.getActive() != null) company.setActive(input.getActive());
        if (input.getEnableViesValidation() != null) company.setEnableViesValidation(input.getEnableViesValidation());
        if (input.getEnableAiMapping() != null) company.setEnableAiMapping(input.getEnableAiMapping());
        if (input.getAutoValidateOnImport() != null) company.setAutoValidateOnImport(input.getAutoValidateOnImport());
        if (input.getPreferredRateProvider() != null) company.setPreferredRateProvider(input.getPreferredRateProvider());
        if (input.getBaseCurrencyId() != null) {
            CurrencyEntity currency = currencyRepository.findById(input.getBaseCurrencyId())
                    .orElseThrow(() -> new IllegalArgumentException("Валутата не е намерена: " + input.getBaseCurrencyId()));
            company.setBaseCurrency(currency);
        }

        // Default сметки за автоматични операции
        updateDefaultAccount(company, input.getDefaultCashAccountId(), company::setDefaultCashAccount, "Каса");
        updateDefaultAccount(company, input.getDefaultCustomersAccountId(), company::setDefaultCustomersAccount, "Клиенти");
        updateDefaultAccount(company, input.getDefaultSuppliersAccountId(), company::setDefaultSuppliersAccount, "Доставчици");
        updateDefaultAccount(company, input.getDefaultSalesRevenueAccountId(), company::setDefaultSalesRevenueAccount, "Приходи от продажби");
        updateDefaultAccount(company, input.getDefaultVatPurchaseAccountId(), company::setDefaultVatPurchaseAccount, "ДДС покупки");
        updateDefaultAccount(company, input.getDefaultVatSalesAccountId(), company::setDefaultVatSalesAccount, "ДДС продажби");
        updateDefaultAccount(company, input.getDefaultCardPaymentPurchaseAccountId(), company::setDefaultCardPaymentPurchaseAccount, "Карта покупки");
        updateDefaultAccount(company, input.getDefaultCardPaymentSalesAccountId(), company::setDefaultCardPaymentSalesAccount, "Карта продажби");

        // Salt Edge Open Banking
        if (input.getSaltEdgeAppId() != null) company.setSaltEdgeAppId(input.getSaltEdgeAppId());
        if (input.getSaltEdgeSecret() != null) company.setSaltEdgeSecret(input.getSaltEdgeSecret());
        if (input.getSaltEdgeEnabled() != null) company.setSaltEdgeEnabled(input.getSaltEdgeEnabled());

        return companyRepository.save(company);
    }

    private void updateDefaultAccount(CompanyEntity company, Integer accountId,
                                       java.util.function.Consumer<AccountEntity> setter, String accountName) {
        if (accountId != null) {
            if (accountId == 0) {
                // Ако е 0, изчистваме връзката
                setter.accept(null);
            } else {
                AccountEntity account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Сметката за " + accountName + " не е намерена: " + accountId));
                // Проверяваме дали сметката принадлежи на същата компания
                if (!account.getCompany().getId().equals(company.getId())) {
                    throw new IllegalArgumentException("Сметката " + account.getCode() + " не принадлежи на тази компания");
                }
                setter.accept(account);
            }
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (!companyRepository.existsById(id)) {
            return false;
        }
        companyRepository.deleteById(id);
        return true;
    }
}
