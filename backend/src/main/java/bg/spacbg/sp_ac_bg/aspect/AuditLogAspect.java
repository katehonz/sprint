package bg.spacbg.sp_ac_bg.aspect;

import bg.spacbg.sp_ac_bg.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // Log report generation - disabled due to read-only transaction conflict
    // The ReportServiceImpl uses @Transactional(readOnly = true) which prevents audit logging
    // @AfterReturning("execution(* bg.spacbg.sp_ac_bg.service.impl.ReportServiceImpl.generate*(..))")
    // public void logReportGeneration(JoinPoint joinPoint) {
    //     try {
    //         String methodName = joinPoint.getSignature().getName();
    //         String reportType = extractReportType(methodName);
    //         Map<String, Object> details = extractInputDetails(joinPoint.getArgs());
    //         auditLogService.logReportGenerate(reportType, details);
    //     } catch (Exception e) {
    //         logger.warn("Failed to log report generation: {}", e.getMessage());
    //     }
    // }

    // Log report export - disabled due to read-only transaction conflict
    // The ReportServiceImpl uses @Transactional(readOnly = true) which prevents audit logging
    // @AfterReturning("execution(* bg.spacbg.sp_ac_bg.service.impl.ReportServiceImpl.export*(..))")
    // public void logReportExport(JoinPoint joinPoint) {
    //     try {
    //         String methodName = joinPoint.getSignature().getName();
    //         String reportType = extractReportType(methodName);
    //         Object[] args = joinPoint.getArgs();
    //         String format = args.length > 1 && args[1] instanceof String ? (String) args[1] : "unknown";
    //         Map<String, Object> details = extractInputDetails(args);
    //         auditLogService.logExport(reportType, format, details);
    //     } catch (Exception e) {
    //         logger.warn("Failed to log report export: {}", e.getMessage());
    //     }
    // }

    // Log VAT return generation
    @AfterReturning("execution(* bg.spacbg.sp_ac_bg.service.impl.VatServiceImpl.generate*(..))")
    public void logVatGeneration(JoinPoint joinPoint) {
        try {
            Map<String, Object> details = extractInputDetails(joinPoint.getArgs());
            auditLogService.logReportGenerate("VAT_RETURN", details);
        } catch (Exception e) {
            logger.warn("Failed to log VAT generation: {}", e.getMessage());
        }
    }

    // Log VAT export
    @AfterReturning("execution(* bg.spacbg.sp_ac_bg.service.impl.VatServiceImpl.export*(..))")
    public void logVatExport(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Map<String, Object> details = extractInputDetails(joinPoint.getArgs());
            auditLogService.logExport("VAT_" + methodName.toUpperCase(), "TXT", details);
        } catch (Exception e) {
            logger.warn("Failed to log VAT export: {}", e.getMessage());
        }
    }

    private String extractReportType(String methodName) {
        // generateTurnoverSheet -> TURNOVER_SHEET
        // exportChronologicalReport -> CHRONOLOGICAL_REPORT
        return methodName
                .replace("generate", "")
                .replace("export", "")
                .replaceAll("([A-Z])", "_$1")
                .toUpperCase()
                .replaceFirst("^_", "");
    }

    private Map<String, Object> extractInputDetails(Object[] args) {
        Map<String, Object> details = new HashMap<>();

        for (Object arg : args) {
            if (arg == null) continue;

            // Try to extract companyId and date fields from input objects
            try {
                Class<?> clazz = arg.getClass();

                // Try getCompanyId
                try {
                    Method getCompanyId = clazz.getMethod("getCompanyId");
                    Object companyId = getCompanyId.invoke(arg);
                    if (companyId != null) {
                        details.put("companyId", companyId);
                    }
                } catch (NoSuchMethodException ignored) {}

                // Try getStartDate
                try {
                    Method getStartDate = clazz.getMethod("getStartDate");
                    Object startDate = getStartDate.invoke(arg);
                    if (startDate != null) {
                        details.put("startDate", startDate.toString());
                    }
                } catch (NoSuchMethodException ignored) {}

                // Try getEndDate
                try {
                    Method getEndDate = clazz.getMethod("getEndDate");
                    Object endDate = getEndDate.invoke(arg);
                    if (endDate != null) {
                        details.put("endDate", endDate.toString());
                    }
                } catch (NoSuchMethodException ignored) {}

                // Try getPeriodYear
                try {
                    Method getPeriodYear = clazz.getMethod("getPeriodYear");
                    Object year = getPeriodYear.invoke(arg);
                    if (year != null) {
                        details.put("year", year);
                    }
                } catch (NoSuchMethodException ignored) {}

                // Try getPeriodMonth
                try {
                    Method getPeriodMonth = clazz.getMethod("getPeriodMonth");
                    Object month = getPeriodMonth.invoke(arg);
                    if (month != null) {
                        details.put("month", month);
                    }
                } catch (NoSuchMethodException ignored) {}

            } catch (Exception e) {
                logger.debug("Could not extract details from argument: {}", e.getMessage());
            }
        }

        return details;
    }
}
