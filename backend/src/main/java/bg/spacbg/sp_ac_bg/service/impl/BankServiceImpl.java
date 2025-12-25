package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.dto.saltedge.SaltEdgeConnectSession;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.model.enums.BankConnectionType;
import bg.spacbg.sp_ac_bg.model.enums.BankImportStatus;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.BankService;
import bg.spacbg.sp_ac_bg.service.S3Service;
import bg.spacbg.sp_ac_bg.service.SaltEdgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankServiceImpl implements BankService {

    private static final Logger log = LoggerFactory.getLogger(BankServiceImpl.class);

    private final BankProfileRepository bankProfileRepository;
    private final BankImportRepository bankImportRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final SaltEdgeService saltEdgeService;

    public BankServiceImpl(
            BankProfileRepository bankProfileRepository,
            BankImportRepository bankImportRepository,
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            S3Service s3Service,
            SaltEdgeService saltEdgeService) {
        this.bankProfileRepository = bankProfileRepository;
        this.bankImportRepository = bankImportRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.saltEdgeService = saltEdgeService;
    }

    // ========== Bank Profile Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<BankProfileEntity> findProfilesByCompanyId(Integer companyId) {
        return bankProfileRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankProfileEntity> findProfileById(Integer id) {
        return bankProfileRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankProfileEntity> findProfileByIban(String iban) {
        return bankProfileRepository.findByIban(iban);
    }

    @Override
    public BankProfileEntity createProfile(CreateBankProfileInput input, Integer userId) {
        if (input.getIban() != null && bankProfileRepository.existsByIban(input.getIban())) {
            throw new IllegalArgumentException("Банков профил с IBAN " + input.getIban() + " вече съществува");
        }

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        AccountEntity account = accountRepository.findById(input.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Сметката не е намерена: " + input.getAccountId()));

        AccountEntity bufferAccount = accountRepository.findById(input.getBufferAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Буферната сметка не е намерена: " + input.getBufferAccountId()));

        UserEntity user = userRepository.findById(userId).orElse(null);

        BankProfileEntity profile = new BankProfileEntity();
        profile.setCompany(company);
        profile.setName(input.getName());
        profile.setIban(input.getIban());
        profile.setAccount(account);
        profile.setBufferAccount(bufferAccount);
        profile.setCurrencyCode(input.getCurrencyCode() != null ? input.getCurrencyCode() : "EUR");

        // Set connection type (default to FILE_IMPORT)
        BankConnectionType connectionType = input.getConnectionType() != null ?
                input.getConnectionType() : BankConnectionType.FILE_IMPORT;
        profile.setConnectionType(connectionType);

        // Set import format for file-based imports
        if (connectionType == BankConnectionType.FILE_IMPORT) {
            profile.setImportFormat(input.getImportFormat());
        }

        // Set Salt Edge info for Open Banking
        if (connectionType == BankConnectionType.SALT_EDGE) {
            profile.setSaltEdgeProviderCode(input.getSaltEdgeProviderCode());
            profile.setSaltEdgeAccountId(input.getSaltEdgeAccountId());
            profile.setSaltEdgeStatus("pending");
        }

        profile.setSettings(input.getSettings());
        profile.setActive(true);
        profile.setCreatedBy(user);

        return bankProfileRepository.save(profile);
    }

    @Override
    public BankProfileEntity updateProfile(Integer id, UpdateBankProfileInput input) {
        BankProfileEntity profile = bankProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + id));

        if (input.getName() != null) profile.setName(input.getName());
        if (input.getIban() != null) {
            if (!input.getIban().equals(profile.getIban()) && bankProfileRepository.existsByIban(input.getIban())) {
                throw new IllegalArgumentException("Банков профил с IBAN " + input.getIban() + " вече съществува");
            }
            profile.setIban(input.getIban());
        }
        if (input.getAccountId() != null) {
            AccountEntity account = accountRepository.findById(input.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Сметката не е намерена"));
            profile.setAccount(account);
        }
        if (input.getBufferAccountId() != null) {
            AccountEntity bufferAccount = accountRepository.findById(input.getBufferAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Буферната сметка не е намерена"));
            profile.setBufferAccount(bufferAccount);
        }
        if (input.getCurrencyCode() != null) profile.setCurrencyCode(input.getCurrencyCode());
        if (input.getImportFormat() != null) profile.setImportFormat(input.getImportFormat());
        if (input.getSettings() != null) profile.setSettings(input.getSettings());
        if (input.getIsActive() != null) profile.setActive(input.getIsActive());

        return bankProfileRepository.save(profile);
    }

    @Override
    public boolean deleteProfile(Integer id) {
        if (!bankProfileRepository.existsById(id)) {
            return false;
        }
        bankProfileRepository.deleteById(id);
        return true;
    }

    // ========== Bank Import Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<BankImportEntity> findImportsByCompanyId(Integer companyId) {
        return bankImportRepository.findByCompanyIdOrderByImportedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankImportEntity> findImportsByProfileId(Integer bankProfileId) {
        return bankImportRepository.findByBankProfileIdOrderByImportedAtDesc(bankProfileId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankImportEntity> findImportById(Integer id) {
        return bankImportRepository.findById(id);
    }

    @Override
    public BankImportEntity processFileImport(Integer bankProfileId, String fileKey, Integer userId) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + bankProfileId));

        if (!profile.isFileImport()) {
            throw new IllegalStateException("Профилът не поддържа файлов импорт");
        }

        UserEntity user = userRepository.findById(userId).orElse(null);

        BankImportEntity importEntity = new BankImportEntity();
        importEntity.setBankProfile(profile);
        importEntity.setCompany(profile.getCompany());
        importEntity.setFileName(fileKey);
        importEntity.setImportFormat(profile.getImportFormat().getValue());
        importEntity.setImportedAt(OffsetDateTime.now());
        importEntity.setStatus(BankImportStatus.IN_PROGRESS);
        importEntity.setTransactionsCount(0);
        importEntity.setTotalCredit(BigDecimal.ZERO);
        importEntity.setTotalDebit(BigDecimal.ZERO);
        importEntity.setCreatedJournalEntries(0);
        importEntity.setCreatedBy(user);

        importEntity = bankImportRepository.save(importEntity);

        try {
            // TODO: Implement actual bank file parsing based on format
            // For now, just mark as completed
            // parseAndProcessBankFile(importEntity, fileKey);

            importEntity.setStatus(BankImportStatus.COMPLETED);
        } catch (Exception e) {
            importEntity.setStatus(BankImportStatus.FAILED);
            importEntity.setErrorMessage(e.getMessage());
        }

        return bankImportRepository.save(importEntity);
    }

    @Override
    public boolean deleteImport(Integer id) {
        if (!bankImportRepository.existsById(id)) {
            return false;
        }
        bankImportRepository.deleteById(id);
        return true;
    }

    // ========== Salt Edge Open Banking Operations ==========

    @Override
    public SaltEdgeConnectSession initiateSaltEdgeConnection(Integer companyId, String providerCode, String returnUrl) {
        log.info("Иницииране на Salt Edge връзка за компания {} с provider {}", companyId, providerCode);
        return saltEdgeService.createConnectSession(companyId, providerCode, returnUrl);
    }

    @Override
    public SaltEdgeConnectSession reconnectSaltEdge(Integer bankProfileId, String returnUrl) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + bankProfileId));

        if (!profile.isOpenBanking()) {
            throw new IllegalStateException("Профилът не е Open Banking профил");
        }

        if (profile.getSaltEdgeConnectionId() == null) {
            throw new IllegalStateException("Профилът няма Salt Edge връзка");
        }

        log.info("Reconnect на Salt Edge за профил {}", bankProfileId);
        return saltEdgeService.createReconnectSession(profile.getSaltEdgeConnectionId(), returnUrl);
    }

    @Override
    public BankProfileEntity linkSaltEdgeAccount(Integer bankProfileId, String saltEdgeAccountId) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + bankProfileId));

        log.info("Свързване на Salt Edge account {} с профил {}", saltEdgeAccountId, bankProfileId);

        saltEdgeService.mapAccountToProfile(saltEdgeAccountId, bankProfileId);

        // Refresh profile from DB
        return bankProfileRepository.findById(bankProfileId).orElse(profile);
    }

    @Override
    public List<SaltEdgeTransactionEntity> syncSaltEdgeTransactions(Integer bankProfileId, LocalDate fromDate, LocalDate toDate) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + bankProfileId));

        if (!profile.isOpenBanking()) {
            throw new IllegalStateException("Профилът не е Open Banking профил");
        }

        log.info("Синхронизиране на транзакции за профил {} от {} до {}", bankProfileId, fromDate, toDate);
        return saltEdgeService.syncTransactions(bankProfileId, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeTransactionEntity> getUnprocessedSaltEdgeTransactions(Integer bankProfileId) {
        return saltEdgeService.getUnprocessedTransactions(bankProfileId);
    }

    @Override
    public BankImportEntity processOpenBankingImport(Integer bankProfileId, Integer userId) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Банковият профил не е намерен: " + bankProfileId));

        if (!profile.isOpenBanking()) {
            throw new IllegalStateException("Профилът не е Open Banking профил");
        }

        UserEntity user = userRepository.findById(userId).orElse(null);

        // Sync latest transactions
        List<SaltEdgeTransactionEntity> newTransactions = saltEdgeService.syncTransactions(
                bankProfileId,
                LocalDate.now().minusMonths(1),
                LocalDate.now()
        );

        // Get all unprocessed transactions
        List<SaltEdgeTransactionEntity> unprocessed = saltEdgeService.getUnprocessedTransactions(bankProfileId);

        // Calculate totals
        BigDecimal totalCredit = unprocessed.stream()
                .filter(SaltEdgeTransactionEntity::isCredit)
                .map(SaltEdgeTransactionEntity::getAbsoluteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebit = unprocessed.stream()
                .filter(SaltEdgeTransactionEntity::isDebit)
                .map(SaltEdgeTransactionEntity::getAbsoluteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create import record
        BankImportEntity importEntity = new BankImportEntity();
        importEntity.setBankProfile(profile);
        importEntity.setCompany(profile.getCompany());
        importEntity.setFileName("open_banking_sync_" + System.currentTimeMillis());
        importEntity.setImportFormat("SALT_EDGE");
        importEntity.setImportedAt(OffsetDateTime.now());
        importEntity.setStatus(BankImportStatus.IN_PROGRESS);
        importEntity.setTransactionsCount(unprocessed.size());
        importEntity.setTotalCredit(totalCredit);
        importEntity.setTotalDebit(totalDebit);
        importEntity.setCreatedJournalEntries(0);
        importEntity.setCreatedBy(user);

        importEntity = bankImportRepository.save(importEntity);

        // TODO: Create journal entries from transactions
        // For now, just mark as completed
        importEntity.setStatus(BankImportStatus.COMPLETED);

        return bankImportRepository.save(importEntity);
    }
}
