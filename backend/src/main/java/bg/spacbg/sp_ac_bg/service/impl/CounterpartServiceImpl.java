package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.CounterpartEntity;
import bg.spacbg.sp_ac_bg.model.enums.CounterpartType;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.repository.CounterpartRepository;
import bg.spacbg.sp_ac_bg.service.CounterpartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CounterpartServiceImpl implements CounterpartService {

    private final CounterpartRepository counterpartRepository;
    private final CompanyRepository companyRepository;

    public CounterpartServiceImpl(CounterpartRepository counterpartRepository, CompanyRepository companyRepository) {
        this.counterpartRepository = counterpartRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CounterpartEntity> findByCompanyId(Integer companyId) {
        return counterpartRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CounterpartEntity> search(Integer companyId, String query) {
        return counterpartRepository.searchByCompanyIdAndQuery(companyId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounterpartEntity> findById(Integer id) {
        return counterpartRepository.findById(id);
    }

    @Override
    public CounterpartEntity create(CreateCounterpartInput input) {
        if (input.getEik() != null && counterpartRepository.existsByCompanyIdAndEik(input.getCompanyId(), input.getEik())) {
            throw new IllegalArgumentException("Контрагент с ЕИК " + input.getEik() + " вече съществува в тази компания");
        }

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        CounterpartEntity counterpart = new CounterpartEntity();
        counterpart.setName(input.getName());
        counterpart.setEik(input.getEik());
        counterpart.setVatNumber(input.getVatNumber());
        counterpart.setAddress(input.getAddress());
        counterpart.setLongAddress(input.getLongAddress());
        counterpart.setCity(input.getCity());
        counterpart.setCountry(input.getCountry() != null ? input.getCountry() : "България");
        counterpart.setCounterpartType(input.getCounterpartType());
        counterpart.setVatRegistered(input.getIsVatRegistered() != null ? input.getIsVatRegistered() : false);
        counterpart.setCompany(company);
        counterpart.setActive(true);

        // Set customer/supplier flags based on type
        counterpart.setCustomer(input.getCounterpartType() == CounterpartType.CUSTOMER);
        counterpart.setSupplier(input.getCounterpartType() == CounterpartType.SUPPLIER);

        return counterpartRepository.save(counterpart);
    }

    @Override
    public CounterpartEntity update(Integer id, UpdateCounterpartInput input) {
        CounterpartEntity counterpart = counterpartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Контрагентът не е намерен: " + id));

        if (input.getName() != null) counterpart.setName(input.getName());
        if (input.getEik() != null) {
            if (!input.getEik().equals(counterpart.getEik()) &&
                    counterpartRepository.existsByCompanyIdAndEik(counterpart.getCompany().getId(), input.getEik())) {
                throw new IllegalArgumentException("Контрагент с ЕИК " + input.getEik() + " вече съществува");
            }
            counterpart.setEik(input.getEik());
        }
        if (input.getVatNumber() != null) counterpart.setVatNumber(input.getVatNumber());
        if (input.getAddress() != null) counterpart.setAddress(input.getAddress());
        if (input.getLongAddress() != null) counterpart.setLongAddress(input.getLongAddress());
        if (input.getCity() != null) counterpart.setCity(input.getCity());
        if (input.getCountry() != null) counterpart.setCountry(input.getCountry());
        if (input.getCounterpartType() != null) {
            counterpart.setCounterpartType(input.getCounterpartType());
            counterpart.setCustomer(input.getCounterpartType() == CounterpartType.CUSTOMER);
            counterpart.setSupplier(input.getCounterpartType() == CounterpartType.SUPPLIER);
        }
        if (input.getIsVatRegistered() != null) counterpart.setVatRegistered(input.getIsVatRegistered());
        if (input.getIsActive() != null) counterpart.setActive(input.getIsActive());

        return counterpartRepository.save(counterpart);
    }

    @Override
    public boolean delete(Integer id) {
        if (!counterpartRepository.existsById(id)) {
            return false;
        }
        counterpartRepository.deleteById(id);
        return true;
    }
}
