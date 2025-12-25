package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.ChartOfAccountsDto;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateAccountInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateAccountInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import bg.spacbg.sp_ac_bg.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public List<AccountEntity> accounts(@Argument Integer companyId) {
        return accountService.findByCompanyId(companyId);
    }

    @QueryMapping
    public AccountEntity account(@Argument Integer id) {
        return accountService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<AccountEntity> accountsByParent(@Argument Integer companyId, @Argument Integer parentId) {
        return accountService.findByParent(companyId, parentId);
    }

    @QueryMapping
    public List<AccountEntity> accountHierarchy(@Argument Integer companyId) {
        return accountService.findHierarchy(companyId);
    }

    @QueryMapping
    public List<AccountEntity> analyticalAccounts(@Argument Integer companyId) {
        return accountService.findAnalytical(companyId);
    }

    @MutationMapping
    public AccountEntity createAccount(@Argument CreateAccountInput input) {
        return accountService.create(input);
    }

    @MutationMapping
    public AccountEntity updateAccount(@Argument Integer id, @Argument UpdateAccountInput input) {
        return accountService.update(id, input);
    }

    @MutationMapping
    public Boolean deleteAccount(@Argument Integer id) {
        return accountService.delete(id);
    }

    // Chart of Accounts Import/Export via GraphQL
    @QueryMapping
    public List<ChartOfAccountsDto> exportChartOfAccounts(@Argument Integer companyId) {
        return accountService.exportChartOfAccounts(companyId);
    }

    @MutationMapping
    public Integer importChartOfAccounts(
            @Argument Integer companyId,
            @Argument List<ChartOfAccountsDto> accounts,
            @Argument Boolean replaceExisting) {
        return accountService.importChartOfAccounts(companyId, accounts, replaceExisting != null && replaceExisting);
    }

    @MutationMapping
    public Integer copyChartOfAccounts(
            @Argument Integer sourceCompanyId,
            @Argument Integer targetCompanyId,
            @Argument Boolean replaceExisting) {
        return accountService.copyChartOfAccounts(sourceCompanyId, targetCompanyId, replaceExisting != null && replaceExisting);
    }
}
