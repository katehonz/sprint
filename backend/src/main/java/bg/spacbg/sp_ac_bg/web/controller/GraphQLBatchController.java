package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.repository.*;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GraphQL Batch Mappings to solve N+1 query problem.
 * Uses @BatchMapping to batch multiple individual queries into single bulk queries.
 */
@Controller
public class GraphQLBatchController {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final CounterpartRepository counterpartRepository;
    private final CurrencyRepository currencyRepository;

    public GraphQLBatchController(
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            CounterpartRepository counterpartRepository,
            CurrencyRepository currencyRepository) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.counterpartRepository = counterpartRepository;
        this.currencyRepository = currencyRepository;
    }

    // ========== JournalEntry Batch Mappings ==========

    @BatchMapping(typeName = "JournalEntry", field = "company")
    public Map<JournalEntryEntity, CompanyEntity> journalEntryCompanies(List<JournalEntryEntity> entries) {
        Set<Integer> companyIds = entries.stream()
                .map(e -> e.getCompany().getId())
                .collect(Collectors.toSet());

        Map<Integer, CompanyEntity> companiesMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(CompanyEntity::getId, Function.identity()));

        return entries.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        e -> companiesMap.get(e.getCompany().getId())
                ));
    }

    @BatchMapping(typeName = "JournalEntry", field = "counterpart")
    public Map<JournalEntryEntity, CounterpartEntity> journalEntryCounterparts(List<JournalEntryEntity> entries) {
        Set<Integer> counterpartIds = entries.stream()
                .filter(e -> e.getCounterpart() != null)
                .map(e -> e.getCounterpart().getId())
                .collect(Collectors.toSet());

        if (counterpartIds.isEmpty()) {
            return entries.stream()
                    .collect(Collectors.toMap(Function.identity(), e -> null));
        }

        Map<Integer, CounterpartEntity> counterpartsMap = counterpartRepository.findAllById(counterpartIds).stream()
                .collect(Collectors.toMap(CounterpartEntity::getId, Function.identity()));

        return entries.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        e -> e.getCounterpart() != null ? counterpartsMap.get(e.getCounterpart().getId()) : null
                ));
    }

    // ========== EntryLine Batch Mappings ==========

    @BatchMapping(typeName = "EntryLine", field = "account")
    public Map<EntryLineEntity, AccountEntity> entryLineAccounts(List<EntryLineEntity> lines) {
        Set<Integer> accountIds = lines.stream()
                .map(l -> l.getAccount().getId())
                .collect(Collectors.toSet());

        Map<Integer, AccountEntity> accountsMap = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));

        return lines.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        l -> accountsMap.get(l.getAccount().getId())
                ));
    }

    @BatchMapping(typeName = "EntryLine", field = "counterpart")
    public Map<EntryLineEntity, CounterpartEntity> entryLineCounterparts(List<EntryLineEntity> lines) {
        Set<Integer> counterpartIds = lines.stream()
                .filter(l -> l.getCounterpart() != null)
                .map(l -> l.getCounterpart().getId())
                .collect(Collectors.toSet());

        if (counterpartIds.isEmpty()) {
            return lines.stream()
                    .collect(Collectors.toMap(Function.identity(), l -> null));
        }

        Map<Integer, CounterpartEntity> counterpartsMap = counterpartRepository.findAllById(counterpartIds).stream()
                .collect(Collectors.toMap(CounterpartEntity::getId, Function.identity()));

        return lines.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        l -> l.getCounterpart() != null ? counterpartsMap.get(l.getCounterpart().getId()) : null
                ));
    }

    // ========== Account Batch Mappings ==========

    @BatchMapping(typeName = "Account", field = "company")
    public Map<AccountEntity, CompanyEntity> accountCompanies(List<AccountEntity> accounts) {
        Set<Integer> companyIds = accounts.stream()
                .map(a -> a.getCompany().getId())
                .collect(Collectors.toSet());

        Map<Integer, CompanyEntity> companiesMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(CompanyEntity::getId, Function.identity()));

        return accounts.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> companiesMap.get(a.getCompany().getId())
                ));
    }

    @BatchMapping(typeName = "Account", field = "parent")
    public Map<AccountEntity, AccountEntity> accountParents(List<AccountEntity> accounts) {
        Set<Integer> parentIds = accounts.stream()
                .filter(a -> a.getParent() != null)
                .map(a -> a.getParent().getId())
                .collect(Collectors.toSet());

        Map<Integer, AccountEntity> parentsMap = parentIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : accountRepository.findAllById(parentIds).stream()
                        .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));

        // Use HashMap which allows null values (Collectors.toMap doesn't)
        Map<AccountEntity, AccountEntity> result = new java.util.HashMap<>();
        for (AccountEntity account : accounts) {
            AccountEntity parent = account.getParent() != null
                    ? parentsMap.get(account.getParent().getId())
                    : null;
            result.put(account, parent);
        }
        return result;
    }

    // ========== Counterpart Batch Mappings ==========

    @BatchMapping(typeName = "Counterpart", field = "company")
    public Map<CounterpartEntity, CompanyEntity> counterpartCompanies(List<CounterpartEntity> counterparts) {
        Set<Integer> companyIds = counterparts.stream()
                .map(c -> c.getCompany().getId())
                .collect(Collectors.toSet());

        Map<Integer, CompanyEntity> companiesMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(CompanyEntity::getId, Function.identity()));

        return counterparts.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        c -> companiesMap.get(c.getCompany().getId())
                ));
    }

    // ========== Company Batch Mappings ==========

    @BatchMapping(typeName = "Company", field = "baseCurrency")
    public Map<CompanyEntity, CurrencyEntity> companyBaseCurrencies(List<CompanyEntity> companies) {
        Set<Integer> currencyIds = companies.stream()
                .filter(c -> c.getBaseCurrency() != null)
                .map(c -> c.getBaseCurrency().getId())
                .collect(Collectors.toSet());

        if (currencyIds.isEmpty()) {
            return companies.stream()
                    .collect(Collectors.toMap(Function.identity(), c -> null));
        }

        Map<Integer, CurrencyEntity> currenciesMap = currencyRepository.findAllById(currencyIds).stream()
                .collect(Collectors.toMap(CurrencyEntity::getId, Function.identity()));

        return companies.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        c -> c.getBaseCurrency() != null ? currenciesMap.get(c.getBaseCurrency().getId()) : null
                ));
    }
}
