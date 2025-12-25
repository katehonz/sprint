package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.dto.saltedge.SaltEdgeConnectSession;
import bg.spacbg.sp_ac_bg.model.entity.BankImportEntity;
import bg.spacbg.sp_ac_bg.model.entity.BankProfileEntity;
import bg.spacbg.sp_ac_bg.model.entity.SaltEdgeTransactionEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankService {
    // Bank Profile operations
    List<BankProfileEntity> findProfilesByCompanyId(Integer companyId);
    Optional<BankProfileEntity> findProfileById(Integer id);
    Optional<BankProfileEntity> findProfileByIban(String iban);
    BankProfileEntity createProfile(CreateBankProfileInput input, Integer userId);
    BankProfileEntity updateProfile(Integer id, UpdateBankProfileInput input);
    boolean deleteProfile(Integer id);

    // Bank Import operations (File-based)
    List<BankImportEntity> findImportsByCompanyId(Integer companyId);
    List<BankImportEntity> findImportsByProfileId(Integer bankProfileId);
    Optional<BankImportEntity> findImportById(Integer id);
    BankImportEntity processFileImport(Integer bankProfileId, String fileKey, Integer userId);
    boolean deleteImport(Integer id);

    // Salt Edge Open Banking operations
    SaltEdgeConnectSession initiateSaltEdgeConnection(Integer companyId, String providerCode, String returnUrl);
    SaltEdgeConnectSession reconnectSaltEdge(Integer bankProfileId, String returnUrl);
    BankProfileEntity linkSaltEdgeAccount(Integer bankProfileId, String saltEdgeAccountId);
    List<SaltEdgeTransactionEntity> syncSaltEdgeTransactions(Integer bankProfileId, LocalDate fromDate, LocalDate toDate);
    List<SaltEdgeTransactionEntity> getUnprocessedSaltEdgeTransactions(Integer bankProfileId);
    BankImportEntity processOpenBankingImport(Integer bankProfileId, Integer userId);
}
