package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.AccountingPeriodFilter;
import bg.spacbg.sp_ac_bg.model.entity.AccountingPeriodEntity;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.UserEntity;
import bg.spacbg.sp_ac_bg.model.enums.PeriodStatus;
import bg.spacbg.sp_ac_bg.repository.AccountingPeriodRepository;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.repository.UserRepository;
import bg.spacbg.sp_ac_bg.service.AccountingPeriodService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private static final Logger log = LoggerFactory.getLogger(AccountingPeriodServiceImpl.class);

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public AccountingPeriodServiceImpl(
            AccountingPeriodRepository accountingPeriodRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodEntity> findByCompanyId(Integer companyId) {
        return accountingPeriodRepository.findByCompany_IdOrderByYearDescMonthDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodEntity> findByFilter(AccountingPeriodFilter filter) {
        if (filter.getYear() != null && filter.getStatus() != null) {
            return accountingPeriodRepository.findByCompany_IdAndYear(filter.getCompanyId(), filter.getYear())
                    .stream()
                    .filter(p -> p.getStatus() == filter.getStatus())
                    .toList();
        } else if (filter.getYear() != null) {
            return accountingPeriodRepository.findByCompany_IdAndYear(filter.getCompanyId(), filter.getYear());
        } else if (filter.getStatus() != null) {
            return accountingPeriodRepository.findByCompany_IdAndStatus(filter.getCompanyId(), filter.getStatus());
        }
        return findByCompanyId(filter.getCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingPeriodEntity> findById(Integer id) {
        return accountingPeriodRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingPeriodEntity> findByCompanyIdAndYearAndMonth(Integer companyId, Integer year, Integer month) {
        return accountingPeriodRepository.findByCompany_IdAndYearAndMonth(companyId, year, month);
    }

    @Override
    public AccountingPeriodEntity closePeriod(Integer companyId, Integer year, Integer month, Integer userId) {
        log.info("Closing accounting period {}/{} for company {}", year, month, companyId);

        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + companyId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен: " + userId));

        Optional<AccountingPeriodEntity> existingPeriod =
                accountingPeriodRepository.findByCompany_IdAndYearAndMonth(companyId, year, month);

        AccountingPeriodEntity period;
        if (existingPeriod.isPresent()) {
            period = existingPeriod.get();
            if (period.getStatus() == PeriodStatus.CLOSED) {
                throw new IllegalArgumentException("Периодът " + month + "/" + year + " вече е приключен");
            }
        } else {
            period = new AccountingPeriodEntity();
            period.setCompany(company);
            period.setYear(year);
            period.setMonth(month);
        }

        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedBy(user);
        period.setClosedAt(OffsetDateTime.now());

        AccountingPeriodEntity saved = accountingPeriodRepository.save(period);
        log.info("Successfully closed accounting period {}/{} for company {}", year, month, companyId);
        return saved;
    }

    @Override
    public AccountingPeriodEntity reopenPeriod(Integer companyId, Integer year, Integer month) {
        log.info("Reopening accounting period {}/{} for company {}", year, month, companyId);

        AccountingPeriodEntity period = accountingPeriodRepository
                .findByCompany_IdAndYearAndMonth(companyId, year, month)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Периодът " + month + "/" + year + " не е намерен"));

        if (period.getStatus() == PeriodStatus.OPEN) {
            throw new IllegalArgumentException("Периодът " + month + "/" + year + " вече е отворен");
        }

        period.setStatus(PeriodStatus.OPEN);
        period.setClosedBy(null);
        period.setClosedAt(null);

        AccountingPeriodEntity saved = accountingPeriodRepository.save(period);
        log.info("Successfully reopened accounting period {}/{} for company {}", year, month, companyId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPeriodOpen(Integer companyId, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        Optional<AccountingPeriodEntity> period =
                accountingPeriodRepository.findByCompany_IdAndYearAndMonth(companyId, year, month);

        // If no period record exists, the period is considered open
        return period.map(p -> p.getStatus() == PeriodStatus.OPEN).orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePeriodIsOpen(Integer companyId, LocalDate date) {
        if (!isPeriodOpen(companyId, date)) {
            int year = date.getYear();
            int month = date.getMonthValue();
            throw new IllegalArgumentException(
                    "Счетоводният период " + month + "/" + year + " е приключен. " +
                    "Не можете да създавате, редактирате или изтривате записи в приключен период.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodStatus getPeriodStatus(Integer companyId, Integer year, Integer month) {
        Optional<AccountingPeriodEntity> period =
                accountingPeriodRepository.findByCompany_IdAndYearAndMonth(companyId, year, month);

        return period.map(AccountingPeriodEntity::getStatus).orElse(PeriodStatus.OPEN);
    }
}
