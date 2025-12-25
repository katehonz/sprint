package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.output.AuditLogsPage;
import bg.spacbg.sp_ac_bg.model.entity.AuditLogEntity;
import bg.spacbg.sp_ac_bg.model.entity.AuditSettingsEntity;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.UserEntity;
import bg.spacbg.sp_ac_bg.repository.AuditLogRepository;
import bg.spacbg.sp_ac_bg.repository.AuditSettingsRepository;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.repository.UserRepository;
import bg.spacbg.sp_ac_bg.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditSettingsRepository auditSettingsRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public AuditLogServiceImpl(
            AuditLogRepository auditLogRepository,
            AuditSettingsRepository auditSettingsRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.auditLogRepository = auditLogRepository;
        this.auditSettingsRepository = auditSettingsRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, String entityId, Map<String, Object> details) {
        doLogAction(action, entityType, entityId, details, true, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, String entityId,
                          Map<String, Object> details, boolean success, String errorMessage) {
        doLogAction(action, entityType, entityId, details, success, errorMessage);
    }

    private void doLogAction(String action, String entityType, String entityId,
                             Map<String, Object> details, boolean success, String errorMessage) {
        try {
            if (!shouldLog(action)) {
                return;
            }

            AuditLogEntity log = new AuditLogEntity();

            // Get current user info
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                log.setUsername(username);

                // Get user role
                String role = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(r -> r.startsWith("ROLE_"))
                        .map(r -> r.substring(5))
                        .findFirst()
                        .orElse(null);
                log.setUserRole(role);

                // Get user entity
                userRepository.findByUsername(username).ifPresent(log::setUser);
            }

            // Get company from details if present
            if (details != null && details.containsKey("companyId")) {
                Object companyIdObj = details.get("companyId");
                Integer companyId = companyIdObj instanceof Integer ? (Integer) companyIdObj
                        : Integer.parseInt(companyIdObj.toString());
                companyRepository.findById(companyId).ifPresent(log::setCompany);
            }

            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);

            // Get IP and User-Agent from request
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLogRepository.save(log);
            logger.debug("Audit log saved: {} - {} - {}", action, entityType, entityId);

        } catch (Exception e) {
            logger.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public void logLogin(String ipAddress, String userAgent, boolean success, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("userAgent", userAgent);
        logAction("LOGIN", "SESSION", null, details, success, errorMessage);
    }

    @Override
    public void logLogout() {
        logAction("LOGOUT", "SESSION", null, null);
    }

    @Override
    public void logReportView(String reportType, Map<String, Object> parameters) {
        Map<String, Object> details = new HashMap<>(parameters != null ? parameters : Map.of());
        details.put("reportType", reportType);
        logAction("VIEW_REPORT", "REPORT", reportType, details);
    }

    @Override
    public void logReportGenerate(String reportType, Map<String, Object> parameters) {
        Map<String, Object> details = new HashMap<>(parameters != null ? parameters : Map.of());
        details.put("reportType", reportType);
        logAction("GENERATE_REPORT", "REPORT", reportType, details);
    }

    @Override
    public void logExport(String exportType, String format, Map<String, Object> parameters) {
        Map<String, Object> details = new HashMap<>(parameters != null ? parameters : Map.of());
        details.put("exportType", exportType);
        details.put("format", format);
        logAction("EXPORT", exportType, null, details);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogsPage findLogs(Integer companyId, Integer userId, String action,
                                   LocalDate fromDate, LocalDate toDate, String search,
                                   int offset, int limit) {
        Specification<AuditLogEntity> spec = buildSpecification(companyId, userId, action, fromDate, toDate, search);

        long totalCount = auditLogRepository.count(spec);

        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogEntity> resultPage = auditLogRepository.findAll(spec, pageable);

        boolean hasMore = (offset + limit) < totalCount;

        return new AuditLogsPage(resultPage.getContent(), totalCount, hasMore);
    }

    private Specification<AuditLogEntity> buildSpecification(Integer companyId, Integer userId, String action,
                                                              LocalDate fromDate, LocalDate toDate, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (action != null && !action.isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        fromDate.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            }

            if (search != null && !search.isEmpty()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                Predicate usernameSearch = cb.like(cb.lower(root.get("username")), searchLower);
                Predicate entityIdSearch = cb.like(cb.lower(root.get("entityId")), searchLower);
                Predicate entityTypeSearch = cb.like(cb.lower(root.get("entityType")), searchLower);
                predicates.add(cb.or(usernameSearch, entityIdSearch, entityTypeSearch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getActionStats(Integer companyId, int days) {
        OffsetDateTime fromDate = OffsetDateTime.now().minusDays(days);
        List<Object[]> stats = auditLogRepository.countByActionForCompany(companyId, fromDate);

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : stats) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM every day
    public int cleanupOldLogs() {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(6);
        int deleted = auditLogRepository.deleteOldLogs(cutoffDate);
        if (deleted > 0) {
            logger.info("Cleaned up {} old audit logs (older than 6 months)", deleted);
        }
        return deleted;
    }

    @Override
    public boolean shouldLog(String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // Still log anonymous actions like failed logins
            return "LOGIN".equals(action);
        }

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> r.startsWith("ROLE_"))
                .map(r -> r.substring(5))
                .findFirst()
                .orElse(null);

        if (role == null) {
            return true; // Log by default if no role found
        }

        // Find applicable settings
        Optional<AuditSettingsEntity> settingsOpt = auditSettingsRepository.findGlobalByRole(role);

        if (settingsOpt.isEmpty()) {
            return true; // Log by default if no settings
        }

        AuditSettingsEntity settings = settingsOpt.get();

        if (!settings.getIsEnabled()) {
            return false;
        }

        return switch (action) {
            case "LOGIN" -> settings.getLogLogin();
            case "LOGOUT" -> settings.getLogLogout();
            case "VIEW_REPORT" -> settings.getLogViewReports();
            case "GENERATE_REPORT" -> settings.getLogGenerateReports();
            case "EXPORT" -> settings.getLogExportData();
            case "CREATE" -> settings.getLogCreate();
            case "UPDATE" -> settings.getLogUpdate();
            case "DELETE" -> settings.getLogDelete();
            default -> true;
        };
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
