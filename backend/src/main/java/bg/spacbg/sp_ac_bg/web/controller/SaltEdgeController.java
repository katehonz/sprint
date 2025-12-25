package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.saltedge.*;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.service.BankService;
import bg.spacbg.sp_ac_bg.service.SaltEdgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saltedge")
public class SaltEdgeController {

    private static final Logger log = LoggerFactory.getLogger(SaltEdgeController.class);

    private final SaltEdgeService saltEdgeService;
    private final BankService bankService;

    public SaltEdgeController(SaltEdgeService saltEdgeService, BankService bankService) {
        this.saltEdgeService = saltEdgeService;
        this.bankService = bankService;
    }

    // ========== Provider Endpoints ==========

    @GetMapping("/providers")
    public ResponseEntity<List<SaltEdgeProvider>> listProviders(
            @RequestParam(defaultValue = "BG") String countryCode) {
        List<SaltEdgeProvider> providers = saltEdgeService.listProviders(countryCode);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/providers/bg")
    public ResponseEntity<List<SaltEdgeProvider>> listBulgarianProviders() {
        List<SaltEdgeProvider> providers = saltEdgeService.listBulgarianProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/providers/{providerCode}")
    public ResponseEntity<SaltEdgeProvider> getProvider(@PathVariable String providerCode) {
        return saltEdgeService.getProvider(providerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Connection Session Endpoints ==========

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> createConnectSession(
            @RequestParam Integer companyId,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String returnUrl) {
        try {
            SaltEdgeConnectSession session = bankService.initiateSaltEdgeConnection(companyId, providerCode, returnUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("connectUrl", session.getConnectUrl());
            response.put("expiresAt", session.getExpiresAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при създаване на connect session", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reconnect/{bankProfileId}")
    public ResponseEntity<Map<String, Object>> createReconnectSession(
            @PathVariable Integer bankProfileId,
            @RequestParam(required = false) String returnUrl) {
        try {
            SaltEdgeConnectSession session = bankService.reconnectSaltEdge(bankProfileId, returnUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("connectUrl", session.getConnectUrl());
            response.put("expiresAt", session.getExpiresAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при reconnect session", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh/{connectionId}")
    public ResponseEntity<Map<String, Object>> createRefreshSession(
            @PathVariable String connectionId,
            @RequestParam(required = false) String returnUrl) {
        try {
            SaltEdgeConnectSession session = saltEdgeService.createRefreshSession(connectionId, returnUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("connectUrl", session.getConnectUrl());
            response.put("expiresAt", session.getExpiresAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при refresh session", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Connection Management ==========

    @GetMapping("/connections/{companyId}")
    public ResponseEntity<List<SaltEdgeConnectionEntity>> listConnections(@PathVariable Integer companyId) {
        List<SaltEdgeConnectionEntity> connections = saltEdgeService.listConnectionsByCompany(companyId);
        return ResponseEntity.ok(connections);
    }

    @PostMapping("/connections/{connectionId}/sync")
    public ResponseEntity<SaltEdgeConnectionEntity> syncConnection(@PathVariable String connectionId) {
        try {
            SaltEdgeConnectionEntity connection = saltEdgeService.syncConnection(connectionId);
            return ResponseEntity.ok(connection);
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на connection", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> removeConnection(@PathVariable String connectionId) {
        try {
            saltEdgeService.removeConnection(connectionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Грешка при премахване на connection", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== Account Management ==========

    @PostMapping("/accounts/{connectionId}/sync")
    public ResponseEntity<List<SaltEdgeAccountEntity>> syncAccounts(@PathVariable String connectionId) {
        try {
            List<SaltEdgeAccountEntity> accounts = saltEdgeService.syncAccounts(connectionId);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на accounts", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/accounts/{connectionId}/unmapped")
    public ResponseEntity<List<SaltEdgeAccountEntity>> listUnmappedAccounts(@PathVariable String connectionId) {
        List<SaltEdgeAccountEntity> accounts = saltEdgeService.listUnmappedAccounts(connectionId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/accounts/{saltEdgeAccountId}/link/{bankProfileId}")
    public ResponseEntity<SaltEdgeAccountEntity> linkAccount(
            @PathVariable String saltEdgeAccountId,
            @PathVariable Integer bankProfileId) {
        try {
            SaltEdgeAccountEntity account = saltEdgeService.mapAccountToProfile(saltEdgeAccountId, bankProfileId);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Грешка при свързване на account", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== Transaction Management ==========

    @PostMapping("/transactions/{bankProfileId}/sync")
    public ResponseEntity<List<SaltEdgeTransactionEntity>> syncTransactions(
            @PathVariable Integer bankProfileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            if (fromDate == null) {
                fromDate = LocalDate.now().minusMonths(1);
            }
            if (toDate == null) {
                toDate = LocalDate.now();
            }

            List<SaltEdgeTransactionEntity> transactions = bankService.syncSaltEdgeTransactions(bankProfileId, fromDate, toDate);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Грешка при синхронизиране на transactions", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/transactions/{bankProfileId}/unprocessed")
    public ResponseEntity<List<SaltEdgeTransactionEntity>> getUnprocessedTransactions(@PathVariable Integer bankProfileId) {
        List<SaltEdgeTransactionEntity> transactions = bankService.getUnprocessedSaltEdgeTransactions(bankProfileId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{bankProfileId}/unprocessed/count")
    public ResponseEntity<Map<String, Long>> countUnprocessedTransactions(@PathVariable Integer bankProfileId) {
        long count = saltEdgeService.countUnprocessedTransactions(bankProfileId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ========== Import Processing ==========

    @PostMapping("/import/{bankProfileId}")
    public ResponseEntity<BankImportEntity> processOpenBankingImport(
            @PathVariable Integer bankProfileId,
            @RequestParam Integer userId) {
        try {
            BankImportEntity importEntity = bankService.processOpenBankingImport(bankProfileId, userId);
            return ResponseEntity.ok(importEntity);
        } catch (Exception e) {
            log.error("Грешка при Open Banking импорт", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== Webhook Endpoint ==========

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody SaltEdgeWebhookPayload payload) {
        log.info("Получен Salt Edge webhook: stage={}",
                payload.getData() != null ? payload.getData().getStage() : "unknown");
        try {
            saltEdgeService.handleWebhook(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Грешка при обработка на webhook", e);
            return ResponseEntity.ok().build(); // Return OK to prevent retries
        }
    }

    // ========== Status Endpoints ==========

    @GetMapping("/status/{connectionId}")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(@PathVariable String connectionId) {
        Map<String, Object> status = new HashMap<>();
        status.put("connectionId", connectionId);
        status.put("isActive", saltEdgeService.isConnectionActive(connectionId));
        status.put("isConsentValid", saltEdgeService.isConsentValid(connectionId));
        return ResponseEntity.ok(status);
    }

    // ========== Callback Endpoint (after user completes bank auth) ==========

    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestParam(required = false) String connection_id,
            @RequestParam(required = false) String error_class,
            @RequestParam(required = false) String error_message) {

        Map<String, String> response = new HashMap<>();

        if (error_class != null) {
            log.warn("Salt Edge callback с грешка: {} - {}", error_class, error_message);
            response.put("status", "error");
            response.put("errorClass", error_class);
            response.put("errorMessage", error_message);
        } else if (connection_id != null) {
            log.info("Salt Edge callback успешен: connectionId={}", connection_id);
            response.put("status", "success");
            response.put("connectionId", connection_id);

            // Trigger sync
            try {
                saltEdgeService.syncConnection(connection_id);
                saltEdgeService.syncAccounts(connection_id);
            } catch (Exception e) {
                log.error("Грешка при sync след callback", e);
            }
        } else {
            response.put("status", "unknown");
        }

        return ResponseEntity.ok(response);
    }
}
