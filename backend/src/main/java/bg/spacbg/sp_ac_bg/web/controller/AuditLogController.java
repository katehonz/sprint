package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.AuditLogFilter;
import bg.spacbg.sp_ac_bg.model.dto.output.AuditLogsPage;
import bg.spacbg.sp_ac_bg.model.entity.AuditLogEntity;
import bg.spacbg.sp_ac_bg.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditLogController(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AuditLogsPage auditLogs(@Argument AuditLogFilter filter) {
        int offset = filter.getOffset() != null ? filter.getOffset() : 0;
        int limit = filter.getLimit() != null ? filter.getLimit() : 50;

        return auditLogService.findLogs(
                filter.getCompanyId(),
                filter.getUserId(),
                filter.getAction(),
                filter.getFromDate(),
                filter.getToDate(),
                filter.getSearch(),
                offset,
                limit
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AuditLogStat> auditLogStats(@Argument Integer companyId, @Argument Integer days) {
        int daysToQuery = days != null ? days : 30;
        Map<String, Long> stats = auditLogService.getActionStats(companyId, daysToQuery);

        return stats.entrySet().stream()
                .map(e -> new AuditLogStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // Custom resolver for details field (JSONB -> String)
    @SchemaMapping(typeName = "AuditLog", field = "details")
    public String getDetails(AuditLogEntity log) {
        if (log.getDetails() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(log.getDetails());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    // DTO for statistics
    public record AuditLogStat(String action, Long count) {}
}
