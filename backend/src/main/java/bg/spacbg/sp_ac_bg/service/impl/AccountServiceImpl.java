package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.ChartOfAccountsDto;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateAccountInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateAccountInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.enums.VatDirection;
import bg.spacbg.sp_ac_bg.repository.AccountRepository;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;

    public AccountServiceImpl(AccountRepository accountRepository, CompanyRepository companyRepository) {
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountEntity> findByCompanyId(Integer companyId) {
        return accountRepository.findAllByCompanyIdOrderByCode(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountEntity> findHierarchy(Integer companyId) {
        return accountRepository.findByCompanyIdAndParentIsNull(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountEntity> findByParent(Integer companyId, Integer parentId) {
        if (parentId == null) {
            return accountRepository.findByCompanyIdAndParentIsNull(companyId);
        }
        return accountRepository.findByCompanyIdAndParentId(companyId, parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountEntity> findAnalytical(Integer companyId) {
        return accountRepository.findByCompanyIdAndIsAnalyticalTrue(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountEntity> findById(Integer id) {
        return accountRepository.findById(id);
    }

    @Override
    public AccountEntity create(CreateAccountInput input) {
        if (accountRepository.existsByCompanyIdAndCode(input.getCompanyId(), input.getCode())) {
            throw new IllegalArgumentException("Сметка с код " + input.getCode() + " вече съществува в тази компания");
        }

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        AccountEntity account = new AccountEntity();
        account.setCode(input.getCode());
        account.setName(input.getName());
        account.setDescription(input.getDescription());
        account.setAccountType(input.getAccountType());
        account.setAccountClass(input.getAccountClass());
        account.setCompany(company);

        // Set parent and calculate level
        if (input.getParentId() != null) {
            AccountEntity parent = accountRepository.findById(input.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Родителската сметка не е намерена: " + input.getParentId()));
            account.setParent(parent);
            account.setLevel(parent.getLevel() + 1);
            account.setAnalytical(true);
        } else {
            account.setLevel(1);
            account.setAnalytical(false);
        }

        account.setVatApplicable(input.getIsVatApplicable() != null ? input.getIsVatApplicable() : false);
        account.setVatDirection(input.getVatDirection() != null ? input.getVatDirection() : VatDirection.NONE);

        // Auto-set supports quantities for material/production accounts (class 2 or 3)
        boolean supportsQty = input.getSupportsQuantities() != null
                ? input.getSupportsQuantities()
                : (input.getAccountClass() == 2 || input.getAccountClass() == 3);
        account.setSupportsQuantities(supportsQty);

        // Auto-set default unit for material/production accounts
        if (input.getDefaultUnit() != null) {
            account.setDefaultUnit(input.getDefaultUnit());
        } else if (supportsQty) {
            account.setDefaultUnit("бр");
        }

        account.setActive(true);

        return accountRepository.save(account);
    }

    @Override
    public AccountEntity update(Integer id, UpdateAccountInput input) {
        AccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сметката не е намерена: " + id));

        if (input.getCode() != null) {
            if (!account.getCode().equals(input.getCode()) &&
                    accountRepository.existsByCompanyIdAndCode(account.getCompany().getId(), input.getCode())) {
                throw new IllegalArgumentException("Сметка с код " + input.getCode() + " вече съществува");
            }
            account.setCode(input.getCode());
        }

        if (input.getName() != null) account.setName(input.getName());
        if (input.getAccountType() != null) account.setAccountType(input.getAccountType());
        if (input.getIsVatApplicable() != null) account.setVatApplicable(input.getIsVatApplicable());
        if (input.getVatDirection() != null) account.setVatDirection(input.getVatDirection());
        if (input.getSupportsQuantities() != null) account.setSupportsQuantities(input.getSupportsQuantities());
        if (input.getDefaultUnit() != null) account.setDefaultUnit(input.getDefaultUnit());
        if (input.getIsActive() != null) account.setActive(input.getIsActive());

        if (input.getParentId() != null) {
            AccountEntity parent = accountRepository.findById(input.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Родителската сметка не е намерена"));
            account.setParent(parent);
            account.setLevel(parent.getLevel() + 1);
            account.setAnalytical(true);
        }

        return accountRepository.save(account);
    }

    @Override
    public boolean delete(Integer id) {
        if (!accountRepository.existsById(id)) {
            return false;
        }
        accountRepository.deleteById(id);
        return true;
    }

    // ========== Chart of Accounts Import/Export ==========

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountsDto> exportChartOfAccounts(Integer companyId) {
        List<AccountEntity> accounts = accountRepository.findAllByCompanyIdOrderByCode(companyId);

        return accounts.stream()
                .map(this::toChartDto)
                .collect(Collectors.toList());
    }

    @Override
    public int importChartOfAccounts(Integer companyId, List<ChartOfAccountsDto> accounts, boolean replaceExisting) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + companyId));

        if (replaceExisting) {
            // Delete existing accounts
            List<AccountEntity> existing = accountRepository.findAllByCompanyIdOrderByCode(companyId);
            accountRepository.deleteAll(existing);
            log.info("Изтрити {} съществуващи сметки за компания {}", existing.size(), companyId);
        }

        // Sort accounts by code length (to ensure parents are created first)
        List<ChartOfAccountsDto> sortedAccounts = accounts.stream()
                .sorted(Comparator.comparingInt(a -> a.getCode().length()))
                .collect(Collectors.toList());

        // Map to track created accounts by code
        Map<String, AccountEntity> createdAccounts = new HashMap<>();

        int importedCount = 0;
        for (ChartOfAccountsDto dto : sortedAccounts) {
            try {
                // Skip if account already exists and not replacing
                if (!replaceExisting && accountRepository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
                    log.debug("Сметка {} вече съществува, пропускане", dto.getCode());
                    continue;
                }

                AccountEntity account = new AccountEntity();
                account.setCompany(company);
                account.setCode(dto.getCode());
                account.setName(dto.getName());
                account.setAccountType(dto.getAccountType());
                account.setAccountClass(dto.getAccountClass() != null ? dto.getAccountClass() : determineAccountClass(dto.getCode()));

                // Find parent by code prefix
                AccountEntity parent = findParentAccount(dto.getCode(), createdAccounts, companyId);
                if (parent != null) {
                    account.setParent(parent);
                    account.setLevel(parent.getLevel() + 1);
                } else {
                    account.setLevel(1);
                }

                // Set analytical flag - accounts with 4+ digit codes are typically analytical
                account.setAnalytical(dto.getIsAnalytical() != null ? dto.getIsAnalytical() : dto.getCode().length() >= 4);

                account.setVatApplicable(dto.getIsVatApplicable() != null ? dto.getIsVatApplicable() : false);
                account.setVatDirection(dto.getVatDirection() != null ? dto.getVatDirection() : VatDirection.NONE);
                account.setSupportsQuantities(dto.getSupportsQuantities() != null ? dto.getSupportsQuantities() : false);
                account.setDefaultUnit(dto.getDefaultUnit());
                account.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);

                AccountEntity saved = accountRepository.save(account);
                createdAccounts.put(saved.getCode(), saved);
                importedCount++;

            } catch (Exception e) {
                log.warn("Грешка при импорт на сметка {}: {}", dto.getCode(), e.getMessage());
            }
        }

        log.info("Импортирани {} сметки за компания {}", importedCount, companyId);
        return importedCount;
    }

    @Override
    public int copyChartOfAccounts(Integer sourceCompanyId, Integer targetCompanyId, boolean replaceExisting) {
        List<ChartOfAccountsDto> sourceAccounts = exportChartOfAccounts(sourceCompanyId);
        return importChartOfAccounts(targetCompanyId, sourceAccounts, replaceExisting);
    }

    private ChartOfAccountsDto toChartDto(AccountEntity entity) {
        ChartOfAccountsDto dto = new ChartOfAccountsDto();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setAccountType(entity.getAccountType());
        dto.setAccountClass(entity.getAccountClass());
        dto.setParentCode(entity.getParent() != null ? entity.getParent().getCode() : null);
        dto.setIsAnalytical(entity.isAnalytical());
        dto.setIsVatApplicable(entity.isVatApplicable());
        dto.setVatDirection(entity.getVatDirection());
        dto.setSupportsQuantities(entity.isSupportsQuantities());
        dto.setDefaultUnit(entity.getDefaultUnit());
        dto.setIsActive(entity.isActive());
        return dto;
    }

    private AccountEntity findParentAccount(String code, Map<String, AccountEntity> createdAccounts, Integer companyId) {
        if (code.length() <= 1) {
            return null;
        }

        // Try to find parent with shorter code (e.g., 5031 -> 503 -> 50 -> 5)
        for (int len = code.length() - 1; len >= 1; len--) {
            String parentCode = code.substring(0, len);

            // First check in already created accounts
            if (createdAccounts.containsKey(parentCode)) {
                return createdAccounts.get(parentCode);
            }

            // Then check in database
            Optional<AccountEntity> existing = accountRepository.findByCompanyIdAndCode(companyId, parentCode);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        return null;
    }

    private Integer determineAccountClass(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        char firstChar = code.charAt(0);
        if (Character.isDigit(firstChar)) {
            return Character.getNumericValue(firstChar);
        }
        return null;
    }
}
