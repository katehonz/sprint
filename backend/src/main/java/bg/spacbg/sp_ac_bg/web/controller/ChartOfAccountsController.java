package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.ChartOfAccountsDto;
import bg.spacbg.sp_ac_bg.service.AccountService;
import bg.spacbg.sp_ac_bg.service.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chart-of-accounts")
public class ChartOfAccountsController {

    private static final Logger log = LoggerFactory.getLogger(ChartOfAccountsController.class);

    private final AccountService accountService;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public ChartOfAccountsController(AccountService accountService, ObjectMapper objectMapper,
                                     JwtService jwtService, UserDetailsService userDetailsService) {
        this.accountService = accountService;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Validate JWT token from query parameter (for downloads where headers can't be set)
     */
    private boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            String username = jwtService.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                return jwtService.isTokenValid(token, userDetails);
            }
        } catch (Exception e) {
            log.warn("Invalid token in query parameter", e);
        }
        return false;
    }

    /**
     * Export chart of accounts as JSON
     * Supports both Authorization header and token query parameter for downloads
     */
    @GetMapping("/export/{companyId}")
    public ResponseEntity<byte[]> exportChartOfAccounts(
            @PathVariable Integer companyId,
            @RequestParam(value = "token", required = false) String token) {
        // Check authentication - first check if already authenticated via SecurityContext (JwtAuthFilter)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                                  !"anonymousUser".equals(auth.getPrincipal());

        // Fall back to query parameter token (for direct downloads where headers can't be set)
        if (!isAuthenticated && token != null) {
            isAuthenticated = isValidToken(token);
        }

        if (!isAuthenticated) {
            log.warn("Unauthorized access attempt to export chart of accounts for company {}", companyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<ChartOfAccountsDto> accounts = accountService.exportChartOfAccounts(companyId);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(accounts);
            byte[] bytes = json.getBytes("UTF-8");

            String filename = "chart-of-accounts-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Грешка при експорт на сметкоплан", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export chart of accounts as JSON (inline response)
     */
    @GetMapping("/{companyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChartOfAccountsDto>> getChartOfAccounts(@PathVariable Integer companyId) {
        List<ChartOfAccountsDto> accounts = accountService.exportChartOfAccounts(companyId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Import chart of accounts from JSON file
     */
    @PostMapping("/import/{companyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> importChartOfAccounts(
            @PathVariable Integer companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceExisting", defaultValue = "false") boolean replaceExisting) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<ChartOfAccountsDto> accounts = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<ChartOfAccountsDto>>() {}
            );

            int importedCount = accountService.importChartOfAccounts(companyId, accounts, replaceExisting);

            response.put("success", true);
            response.put("importedCount", importedCount);
            response.put("totalInFile", accounts.size());
            response.put("message", "Успешно импортирани " + importedCount + " сметки");

            log.info("Импортирани {} сметки за компания {} от файл {}", importedCount, companyId, file.getOriginalFilename());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при импорт на сметкоплан", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Import chart of accounts from JSON body
     */
    @PostMapping("/import/{companyId}/json")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> importChartOfAccountsFromJson(
            @PathVariable Integer companyId,
            @RequestBody List<ChartOfAccountsDto> accounts,
            @RequestParam(value = "replaceExisting", defaultValue = "false") boolean replaceExisting) {

        Map<String, Object> response = new HashMap<>();

        try {
            int importedCount = accountService.importChartOfAccounts(companyId, accounts, replaceExisting);

            response.put("success", true);
            response.put("importedCount", importedCount);
            response.put("totalInRequest", accounts.size());
            response.put("message", "Успешно импортирани " + importedCount + " сметки");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при импорт на сметкоплан", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Copy chart of accounts from one company to another
     */
    @PostMapping("/copy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> copyChartOfAccounts(
            @RequestParam Integer sourceCompanyId,
            @RequestParam Integer targetCompanyId,
            @RequestParam(value = "replaceExisting", defaultValue = "false") boolean replaceExisting) {

        Map<String, Object> response = new HashMap<>();

        try {
            int copiedCount = accountService.copyChartOfAccounts(sourceCompanyId, targetCompanyId, replaceExisting);

            response.put("success", true);
            response.put("copiedCount", copiedCount);
            response.put("message", "Успешно копирани " + copiedCount + " сметки от компания " +
                    sourceCompanyId + " в компания " + targetCompanyId);

            log.info("Копирани {} сметки от компания {} в компания {}", copiedCount, sourceCompanyId, targetCompanyId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Грешка при копиране на сметкоплан", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
