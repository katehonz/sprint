package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.saltedge.*;
import bg.spacbg.sp_ac_bg.model.entity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaltEdgeService {

    // Customer Management
    SaltEdgeCustomerEntity getOrCreateCustomer(Integer companyId);
    Optional<SaltEdgeCustomerEntity> findCustomerByCompanyId(Integer companyId);

    // Provider Management
    List<SaltEdgeProvider> listProviders(String countryCode);
    Optional<SaltEdgeProvider> getProvider(String providerCode);
    List<SaltEdgeProvider> listBulgarianProviders();

    // Connection Management
    SaltEdgeConnectSession createConnectSession(Integer companyId, String providerCode, String returnUrl);
    SaltEdgeConnectSession createReconnectSession(String connectionId, String returnUrl);
    SaltEdgeConnectSession createRefreshSession(String connectionId, String returnUrl);
    SaltEdgeConnectionEntity syncConnection(String connectionId);
    void removeConnection(String connectionId);
    List<SaltEdgeConnectionEntity> listConnectionsByCompany(Integer companyId);

    // Account Management
    List<SaltEdgeAccountEntity> syncAccounts(String connectionId);
    List<SaltEdgeAccountEntity> listUnmappedAccounts(String connectionId);
    SaltEdgeAccountEntity mapAccountToProfile(String saltEdgeAccountId, Integer bankProfileId);

    // Transaction Management
    List<SaltEdgeTransactionEntity> syncTransactions(Integer bankProfileId, LocalDate fromDate, LocalDate toDate);
    List<SaltEdgeTransactionEntity> getUnprocessedTransactions(Integer bankProfileId);
    long countUnprocessedTransactions(Integer bankProfileId);

    // Webhook handling
    void handleWebhook(SaltEdgeWebhookPayload payload);

    // Status checks
    boolean isConnectionActive(String connectionId);
    boolean isConsentValid(String connectionId);
}
