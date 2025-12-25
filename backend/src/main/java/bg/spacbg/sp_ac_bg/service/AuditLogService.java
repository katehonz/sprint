package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.output.AuditLogsPage;
import bg.spacbg.sp_ac_bg.model.entity.AuditLogEntity;

import java.time.LocalDate;
import java.util.Map;

public interface AuditLogService {

    // Логване на действия
    void logAction(String action, String entityType, String entityId, Map<String, Object> details);
    void logAction(String action, String entityType, String entityId, Map<String, Object> details, boolean success, String errorMessage);
    void logLogin(String ipAddress, String userAgent, boolean success, String errorMessage);
    void logLogout();
    void logReportView(String reportType, Map<String, Object> parameters);
    void logReportGenerate(String reportType, Map<String, Object> parameters);
    void logExport(String exportType, String format, Map<String, Object> parameters);

    // Търсене на логове
    AuditLogsPage findLogs(Integer companyId, Integer userId, String action,
                           LocalDate fromDate, LocalDate toDate, String search,
                           int offset, int limit);

    // Статистика
    Map<String, Long> getActionStats(Integer companyId, int days);

    // Почистване на стари логове
    int cleanupOldLogs();

    // Проверка дали действие трябва да се логва
    boolean shouldLog(String action);
}
