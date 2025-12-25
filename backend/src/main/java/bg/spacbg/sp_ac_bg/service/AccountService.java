package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.ChartOfAccountsDto;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateAccountInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateAccountInput;
import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;

import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<AccountEntity> findByCompanyId(Integer companyId);
    List<AccountEntity> findHierarchy(Integer companyId);
    List<AccountEntity> findByParent(Integer companyId, Integer parentId);
    List<AccountEntity> findAnalytical(Integer companyId);
    Optional<AccountEntity> findById(Integer id);
    AccountEntity create(CreateAccountInput input);
    AccountEntity update(Integer id, UpdateAccountInput input);
    boolean delete(Integer id);

    // Chart of Accounts Import/Export
    List<ChartOfAccountsDto> exportChartOfAccounts(Integer companyId);
    int importChartOfAccounts(Integer companyId, List<ChartOfAccountsDto> accounts, boolean replaceExisting);
    int copyChartOfAccounts(Integer sourceCompanyId, Integer targetCompanyId, boolean replaceExisting);
}
