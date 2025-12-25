package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.config.SaltEdgeConfig;
import bg.spacbg.sp_ac_bg.model.dto.saltedge.*;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.SaltEdgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class SaltEdgeServiceImpl implements SaltEdgeService {

    private static final Logger log = LoggerFactory.getLogger(SaltEdgeServiceImpl.class);

    private final SaltEdgeConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final BankProfileRepository bankProfileRepository;
    private final SaltEdgeCustomerRepository customerRepository;
    private final SaltEdgeConnectionRepository connectionRepository;
    private final SaltEdgeAccountRepository accountRepository;
    private final SaltEdgeTransactionRepository transactionRepository;

    public SaltEdgeServiceImpl(
            SaltEdgeConfig config,
            CompanyRepository companyRepository,
            BankProfileRepository bankProfileRepository,
            SaltEdgeCustomerRepository customerRepository,
            SaltEdgeConnectionRepository connectionRepository,
            SaltEdgeAccountRepository accountRepository,
            SaltEdgeTransactionRepository transactionRepository) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.companyRepository = companyRepository;
        this.bankProfileRepository = bankProfileRepository;
        this.customerRepository = customerRepository;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // ========== Customer Management ==========

    @Override
    public SaltEdgeCustomerEntity getOrCreateCustomer(Integer companyId) {
        return customerRepository.findByCompanyId(companyId)
                .orElseGet(() -> createCustomer(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SaltEdgeCustomerEntity> findCustomerByCompanyId(Integer companyId) {
        return customerRepository.findByCompanyId(companyId);
    }

    private SaltEdgeCustomerEntity createCustomer(Integer companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + companyId));

        if (!company.isSaltEdgeEnabled()) {
            throw new IllegalStateException("Salt Edge не е активиран за компания: " + company.getName());
        }

        String identifier = "company_" + companyId + "_" + System.currentTimeMillis();

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("identifier", identifier);
        requestBody.put("data", data);

        try {
            ResponseEntity<JsonNode> response = makeApiCallForCompany(
                    HttpMethod.POST,
                    "/customers",
                    requestBody,
                    companyId
            );

            JsonNode customerData = response.getBody().get("data");
            SaltEdgeCustomer seCustomer = objectMapper.treeToValue(customerData, SaltEdgeCustomer.class);

            SaltEdgeCustomerEntity entity = new SaltEdgeCustomerEntity();
            entity.setCompany(company);
            entity.setSaltEdgeCustomerId(seCustomer.getId());
            entity.setIdentifier(seCustomer.getIdentifier());
            entity.setSecret(seCustomer.getSecret());

            return customerRepository.save(entity);
        } catch (Exception e) {
            log.error("Грешка при създаване на Salt Edge customer за компания {}", companyId, e);
            throw new RuntimeException("Не може да се създаде Salt Edge customer", e);
        }
    }

    // ========== Provider Management ==========

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeProvider> listProviders(String countryCode) {
        try {
            String url = "/providers?country_code=" + countryCode;
            ResponseEntity<JsonNode> response = makeApiCall(HttpMethod.GET, url, null);

            JsonNode data = response.getBody().get("data");
            return objectMapper.convertValue(data, new TypeReference<List<SaltEdgeProvider>>() {});
        } catch (Exception e) {
            log.error("Грешка при извличане на providers за държава {}", countryCode, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SaltEdgeProvider> getProvider(String providerCode) {
        try {
            String url = "/providers/" + providerCode;
            ResponseEntity<JsonNode> response = makeApiCall(HttpMethod.GET, url, null);

            JsonNode data = response.getBody().get("data");
            return Optional.of(objectMapper.treeToValue(data, SaltEdgeProvider.class));
        } catch (Exception e) {
            log.error("Грешка при извличане на provider {}", providerCode, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeProvider> listBulgarianProviders() {
        return listProviders("BG");
    }

    // ========== Connection Management ==========

    @Override
    public SaltEdgeConnectSession createConnectSession(Integer companyId, String providerCode, String returnUrl) {
        SaltEdgeCustomerEntity customer = getOrCreateCustomer(companyId);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("customer_id", customer.getSaltEdgeCustomerId());
        data.put("consent", Map.of(
                "scopes", List.of("account_details", "transactions_details"),
                "from_date", LocalDate.now().minusYears(1).toString()
        ));
        data.put("attempt", Map.of(
                "return_to", returnUrl != null ? returnUrl : config.getReturnUrl()
        ));
        if (providerCode != null) {
            data.put("provider_code", providerCode);
        }
        data.put("daily_refresh", true);
        requestBody.put("data", data);

        try {
            ResponseEntity<JsonNode> response = makeApiCallForCompany(
                    HttpMethod.POST,
                    "/connect_sessions/create",
                    requestBody,
                    companyId
            );

            JsonNode sessionData = response.getBody().get("data");
            return objectMapper.treeToValue(sessionData, SaltEdgeConnectSession.class);
        } catch (Exception e) {
            log.error("Грешка при създаване на connect session за компания {}", companyId, e);
            throw new RuntimeException("Не може да се създаде connect session", e);
        }
    }

    @Override
    public SaltEdgeConnectSession createReconnectSession(String connectionId, String returnUrl) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("connection_id", connectionId);
        data.put("consent", Map.of(
                "scopes", List.of("account_details", "transactions_details"),
                "from_date", LocalDate.now().minusYears(1).toString()
        ));
        data.put("attempt", Map.of(
                "return_to", returnUrl != null ? returnUrl : config.getReturnUrl()
        ));
        data.put("daily_refresh", true);
        requestBody.put("data", data);

        try {
            ResponseEntity<JsonNode> response = makeApiCall(
                    HttpMethod.POST,
                    "/connect_sessions/reconnect",
                    requestBody
            );

            JsonNode sessionData = response.getBody().get("data");
            return objectMapper.treeToValue(sessionData, SaltEdgeConnectSession.class);
        } catch (Exception e) {
            log.error("Грешка при reconnect session за connection {}", connectionId, e);
            throw new RuntimeException("Не може да се създаде reconnect session", e);
        }
    }

    @Override
    public SaltEdgeConnectSession createRefreshSession(String connectionId, String returnUrl) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("connection_id", connectionId);
        data.put("attempt", Map.of(
                "return_to", returnUrl != null ? returnUrl : config.getReturnUrl()
        ));
        requestBody.put("data", data);

        try {
            ResponseEntity<JsonNode> response = makeApiCall(
                    HttpMethod.POST,
                    "/connect_sessions/refresh",
                    requestBody
            );

            JsonNode sessionData = response.getBody().get("data");
            return objectMapper.treeToValue(sessionData, SaltEdgeConnectSession.class);
        } catch (Exception e) {
            log.error("Грешка при refresh session за connection {}", connectionId, e);
            throw new RuntimeException("Не може да се създаде refresh session", e);
        }
    }

    @Override
    public SaltEdgeConnectionEntity syncConnection(String connectionId) {
        try {
            String url = "/connections/" + connectionId;
            ResponseEntity<JsonNode> response = makeApiCall(HttpMethod.GET, url, null);

            JsonNode connectionData = response.getBody().get("data");
            SaltEdgeConnection seConnection = objectMapper.treeToValue(connectionData, SaltEdgeConnection.class);

            SaltEdgeConnectionEntity entity = connectionRepository
                    .findBySaltEdgeConnectionId(connectionId)
                    .orElse(new SaltEdgeConnectionEntity());

            entity.setSaltEdgeConnectionId(seConnection.getId());
            entity.setSaltEdgeCustomerId(seConnection.getCustomerId());
            entity.setProviderId(seConnection.getProviderId());
            entity.setProviderCode(seConnection.getProviderCode());
            entity.setProviderName(seConnection.getProviderName());
            entity.setCountryCode(seConnection.getCountryCode());
            entity.setStatus(seConnection.getStatus());
            entity.setLastSuccessAt(seConnection.getLastSuccessAt());
            entity.setNextRefreshPossibleAt(seConnection.getNextRefreshPossibleAt());
            entity.setDailyRefresh(seConnection.getDailyRefresh());
            entity.setConsentId(seConnection.getLastConsentId());

            return connectionRepository.save(entity);
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на connection {}", connectionId, e);
            throw new RuntimeException("Не може да се синхронизира connection", e);
        }
    }

    @Override
    public void removeConnection(String connectionId) {
        try {
            String url = "/connections/" + connectionId;
            makeApiCall(HttpMethod.DELETE, url, null);

            connectionRepository.findBySaltEdgeConnectionId(connectionId)
                    .ifPresent(connectionRepository::delete);
        } catch (Exception e) {
            log.error("Грешка при премахване на connection {}", connectionId, e);
            throw new RuntimeException("Не може да се премахне connection", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeConnectionEntity> listConnectionsByCompany(Integer companyId) {
        return connectionRepository.findByCompanyId(companyId);
    }

    // ========== Account Management ==========

    @Override
    public List<SaltEdgeAccountEntity> syncAccounts(String connectionId) {
        try {
            String url = "/accounts?connection_id=" + connectionId;
            ResponseEntity<JsonNode> response = makeApiCall(HttpMethod.GET, url, null);

            JsonNode accountsData = response.getBody().get("data");
            List<SaltEdgeAccount> seAccounts = objectMapper.convertValue(
                    accountsData, new TypeReference<List<SaltEdgeAccount>>() {});

            List<SaltEdgeAccountEntity> entities = new ArrayList<>();
            for (SaltEdgeAccount seAccount : seAccounts) {
                SaltEdgeAccountEntity entity = accountRepository
                        .findBySaltEdgeAccountId(seAccount.getId())
                        .orElse(new SaltEdgeAccountEntity());

                entity.setSaltEdgeConnectionId(connectionId);
                entity.setSaltEdgeAccountId(seAccount.getId());
                entity.setName(seAccount.getName());
                entity.setNature(seAccount.getNature());
                entity.setCurrencyCode(seAccount.getCurrencyCode());
                entity.setBalance(seAccount.getBalance());
                entity.setAvailableAmount(seAccount.getAvailableAmount());
                entity.setIban(seAccount.getIban());
                entity.setSwift(seAccount.getSwift());
                entity.setAccountNumber(seAccount.getAccountNumber());

                if (seAccount.getExtra() != null) {
                    entity.setExtraData(objectMapper.writeValueAsString(seAccount.getExtra()));
                }

                entities.add(accountRepository.save(entity));
            }

            return entities;
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на accounts за connection {}", connectionId, e);
            throw new RuntimeException("Не може да се синхронизират accounts", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeAccountEntity> listUnmappedAccounts(String connectionId) {
        return accountRepository.findBySaltEdgeConnectionIdAndIsMappedFalse(connectionId);
    }

    @Override
    public SaltEdgeAccountEntity mapAccountToProfile(String saltEdgeAccountId, Integer bankProfileId) {
        SaltEdgeAccountEntity account = accountRepository.findBySaltEdgeAccountId(saltEdgeAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Salt Edge account не е намерен: " + saltEdgeAccountId));

        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Bank profile не е намерен: " + bankProfileId));

        account.setBankProfile(profile);
        account.setIsMapped(true);

        // Update bank profile with Salt Edge info
        profile.setSaltEdgeAccountId(saltEdgeAccountId);
        profile.setSaltEdgeConnectionId(account.getSaltEdgeConnectionId());
        if (account.getIban() != null && profile.getIban() == null) {
            profile.setIban(account.getIban());
        }
        bankProfileRepository.save(profile);

        return accountRepository.save(account);
    }

    // ========== Transaction Management ==========

    @Override
    public List<SaltEdgeTransactionEntity> syncTransactions(Integer bankProfileId, LocalDate fromDate, LocalDate toDate) {
        BankProfileEntity profile = bankProfileRepository.findById(bankProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Bank profile не е намерен: " + bankProfileId));

        if (profile.getSaltEdgeAccountId() == null) {
            throw new IllegalStateException("Bank profile не е свързан със Salt Edge account");
        }

        try {
            String url = "/transactions?account_id=" + profile.getSaltEdgeAccountId();
            if (fromDate != null) {
                url += "&from_date=" + fromDate;
            }
            if (toDate != null) {
                url += "&to_date=" + toDate;
            }

            ResponseEntity<JsonNode> response = makeApiCall(HttpMethod.GET, url, null);

            JsonNode transactionsData = response.getBody().get("data");
            List<SaltEdgeTransaction> seTransactions = objectMapper.convertValue(
                    transactionsData, new TypeReference<List<SaltEdgeTransaction>>() {});

            List<SaltEdgeTransactionEntity> entities = new ArrayList<>();
            for (SaltEdgeTransaction seTxn : seTransactions) {
                if (transactionRepository.existsBySaltEdgeTransactionId(seTxn.getId())) {
                    continue; // Skip duplicates
                }

                SaltEdgeTransactionEntity entity = new SaltEdgeTransactionEntity();
                entity.setSaltEdgeAccountId(seTxn.getAccountId());
                entity.setSaltEdgeTransactionId(seTxn.getId());
                entity.setBankProfile(profile);
                entity.setMadeOn(seTxn.getMadeOn());
                entity.setAmount(seTxn.getAmount());
                entity.setCurrencyCode(seTxn.getCurrencyCode());
                entity.setDescription(seTxn.getDescription());
                entity.setCategory(seTxn.getCategory());
                entity.setMode(seTxn.getMode());
                entity.setStatus(seTxn.getStatus());
                entity.setDuplicated("true".equalsIgnoreCase(seTxn.getDuplicated()));
                entity.setIsProcessed(false);

                if (seTxn.getExtra() != null) {
                    entity.setExtraData(objectMapper.writeValueAsString(seTxn.getExtra()));
                }

                entities.add(transactionRepository.save(entity));
            }

            // Update last sync time
            profile.setSaltEdgeLastSyncAt(OffsetDateTime.now());
            bankProfileRepository.save(profile);

            return entities;
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на transactions за profile {}", bankProfileId, e);
            throw new RuntimeException("Не може да се синхронизират transactions", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaltEdgeTransactionEntity> getUnprocessedTransactions(Integer bankProfileId) {
        return transactionRepository.findByBankProfileIdAndIsProcessedFalse(bankProfileId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnprocessedTransactions(Integer bankProfileId) {
        return transactionRepository.countUnprocessedByBankProfile(bankProfileId);
    }

    // ========== Webhook Handling ==========

    @Override
    public void handleWebhook(SaltEdgeWebhookPayload payload) {
        if (payload == null || payload.getData() == null) {
            log.warn("Получен невалиден webhook payload");
            return;
        }

        String connectionId = payload.getData().getConnectionId();
        String stage = payload.getData().getStage();

        log.info("Обработка на webhook: connectionId={}, stage={}", connectionId, stage);

        switch (stage) {
            case "finish":
                handleConnectionFinished(connectionId);
                break;
            case "error":
                handleConnectionError(connectionId, payload.getData().getErrorClass(),
                        payload.getData().getErrorMessage());
                break;
            case "fetch_accounts":
                syncAccounts(connectionId);
                break;
            case "fetch_recent":
            case "fetch_full":
                handleTransactionsFetched(connectionId);
                break;
            default:
                log.debug("Webhook stage {} не изисква действие", stage);
        }
    }

    private void handleConnectionFinished(String connectionId) {
        try {
            SaltEdgeConnectionEntity connection = syncConnection(connectionId);
            syncAccounts(connectionId);

            // Sync transactions for all mapped profiles
            accountRepository.findBySaltEdgeConnectionId(connectionId).stream()
                    .filter(SaltEdgeAccountEntity::getIsMapped)
                    .filter(a -> a.getBankProfile() != null)
                    .forEach(account -> {
                        syncTransactions(account.getBankProfile().getId(),
                                LocalDate.now().minusMonths(3), LocalDate.now());
                    });

            log.info("Connection {} успешно завършен", connectionId);
        } catch (Exception e) {
            log.error("Грешка при обработка на finish за connection {}", connectionId, e);
        }
    }

    private void handleConnectionError(String connectionId, String errorClass, String errorMessage) {
        connectionRepository.findBySaltEdgeConnectionId(connectionId).ifPresent(connection -> {
            connection.setStatus("error");
            connection.setErrorClass(errorClass);
            connection.setErrorMessage(errorMessage);
            connectionRepository.save(connection);
        });

        log.error("Connection {} грешка: {} - {}", connectionId, errorClass, errorMessage);
    }

    private void handleTransactionsFetched(String connectionId) {
        accountRepository.findBySaltEdgeConnectionId(connectionId).stream()
                .filter(SaltEdgeAccountEntity::getIsMapped)
                .filter(a -> a.getBankProfile() != null)
                .forEach(account -> {
                    syncTransactions(account.getBankProfile().getId(),
                            LocalDate.now().minusMonths(1), LocalDate.now());
                });
    }

    // ========== Status Checks ==========

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectionActive(String connectionId) {
        return connectionRepository.findBySaltEdgeConnectionId(connectionId)
                .map(SaltEdgeConnectionEntity::isActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConsentValid(String connectionId) {
        return connectionRepository.findBySaltEdgeConnectionId(connectionId)
                .map(SaltEdgeConnectionEntity::hasValidConsent)
                .orElse(false);
    }

    // ========== API Helper ==========

    private ResponseEntity<JsonNode> makeApiCall(HttpMethod method, String endpoint, Object body) {
        // Use global config (fallback)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("App-id", config.getAppId());
        headers.set("Secret", config.getSecret());

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        String url = config.getBaseUrl() + endpoint;
        return restTemplate.exchange(url, method, entity, JsonNode.class);
    }

    private ResponseEntity<JsonNode> makeApiCallForCompany(HttpMethod method, String endpoint, Object body, Integer companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + companyId));

        if (!company.isSaltEdgeEnabled()) {
            throw new IllegalStateException("Salt Edge не е активиран за тази компания");
        }

        String appId = company.getSaltEdgeAppId();
        String secret = company.getSaltEdgeSecret();

        if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException("Salt Edge ключовете не са конфигурирани за тази компания");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("App-id", appId);
        headers.set("Secret", secret);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        String url = config.getBaseUrl() + endpoint;
        return restTemplate.exchange(url, method, entity, JsonNode.class);
    }

    private String getAppIdForCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyEntity::getSaltEdgeAppId)
                .orElse(config.getAppId());
    }

    private String getSecretForCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyEntity::getSaltEdgeSecret)
                .orElse(config.getSecret());
    }
}
