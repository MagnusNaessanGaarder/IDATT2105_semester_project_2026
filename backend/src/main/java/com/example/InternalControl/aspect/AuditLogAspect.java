package com.example.InternalControl.aspect;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditLogAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  public AuditLogAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
    this.auditLogService = auditLogService;
    this.objectMapper = objectMapper;
  }

  @Around("@annotation(audited)")
  public Object logAuditEvent(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    // Get authentication early
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      log.debug("No authenticated user, skipping audit log");
      return joinPoint.proceed();
    }

    Long userId = extractUserId(authentication);
    if (userId == null) {
      log.debug("Could not extract user ID, skipping audit log");
      return joinPoint.proceed();
    }

    Integer orgNumber = extractOrgNumber(joinPoint);
    Object oldEntity = null;

    // For UPDATE operations, fetch the old entity BEFORE the change
    if (audited.action() == ActionType.UPDATE) {
      try {
        oldEntity = fetchOldEntity(joinPoint, audited.entityType());
      } catch (Exception e) {
        log.debug("Could not fetch old entity for audit: {}", e.getMessage());
      }
    }

    // Execute the actual method
    Object result;
    try {
      result = joinPoint.proceed();
    } catch (Throwable t) {
      // Log failed operations too
      if (audited.action() == ActionType.DELETE) {
        logFailedAction(orgNumber, userId, audited, joinPoint, t);
      }
      throw t;
    }

    // Extract entity ID from result
    Long entityId = extractEntityId(result, audited.entityId());

    String ipAddress = getClientIpAddress();
    String userAgent = getUserAgent();

    // Serialize old and new values
    String oldValuesJson = null;
    String newValuesJson = null;

    try {
      if (oldEntity != null && audited.action() == ActionType.UPDATE) {
        oldValuesJson = objectMapper.writeValueAsString(oldEntity);
      }
      if (result != null && (audited.action() == ActionType.CREATE || audited.action() == ActionType.UPDATE)) {
        newValuesJson = objectMapper.writeValueAsString(result);
      }
    } catch (Exception e) {
      log.warn("Could not serialize values to JSON for audit log", e);
    }

    // Log the audit
    auditLogService.logAction(
        orgNumber,
        userId,
        audited.action(),
        audited.entityType(),
        entityId,
        oldValuesJson,
        newValuesJson,
        ipAddress,
        userAgent);

    return result;
  }

  private Long extractUserId(Authentication authentication) {
    if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
      return userDetails.getUserId();
    }
    return null;
  }

  private Integer extractOrgNumber(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    for (Object arg : args) {
      if (arg instanceof Integer intValue) {
        if (intValue > 0 && intValue < 1000000000) {
          return intValue;
        }
      }
    }
    return null;
  }

  /**
   * Fetch the old entity from database before update.
   * This is needed to capture the "before" state for audit logs.
   */
  private Object fetchOldEntity(ProceedingJoinPoint joinPoint, String entityType) {
    Object[] args = joinPoint.getArgs();

    try {
      return switch (entityType) {
        case "DeviationReport" -> fetchDeviationReport(args);
        case "TrainingRecord" -> fetchTrainingRecord(args);
        case "ChecklistTemplate" -> fetchChecklistTemplate(args);
        case "ChecklistRun" -> fetchChecklistRun(args);
        case "TemperatureLogEntry" -> fetchTemperatureLogEntry(args);
        case "Location" -> fetchLocation(args);
        case "User" -> fetchUser(args);
        default -> null;
      };
    } catch (Exception e) {
      log.debug("Could not fetch old entity of type {}: {}", entityType, e.getMessage());
      return null;
    }
  }

  private DeviationReport fetchDeviationReport(Object[] args) {
    // Find reportId and orgNumber from args
    Long reportId = null;
    Integer orgNumber = null;

    for (Object arg : args) {
      if (arg instanceof Long l && reportId == null) {
        reportId = l;
      } else if (arg instanceof Integer i && orgNumber == null && i > 0 && i < 1000000000) {
        orgNumber = i;
      }
    }

    if (reportId != null) {
      // We need to access the repository - this requires dependency injection
      // For now, return null and let services handle manual audit logging
      log.debug("Cannot fetch DeviationReport {} without repository access", reportId);
    }
    return null;
  }

  private Object fetchTrainingRecord(Object[] args) {
    // Similar pattern - requires repository access
    return null;
  }

  private Object fetchChecklistTemplate(Object[] args) {
    Long templateId = null;
    for (Object arg : args) {
      if (arg instanceof Long l) {
        templateId = l;
        break;
      }
    }
    log.debug("Cannot fetch ChecklistTemplate {} without repository access", templateId);
    return null;
  }

  private Object fetchChecklistRun(Object[] args) {
    return null;
  }

  private Object fetchTemperatureLogEntry(Object[] args) {
    return null;
  }

  private Object fetchLocation(Object[] args) {
    return null;
  }

  private Object fetchUser(Object[] args) {
    return null;
  }

  private Long extractEntityId(Object result, String entityIdExpression) {
    if (result == null || entityIdExpression.isEmpty()) {
      return null;
    }

    try {
      if (result instanceof DeviationReport report) {
        return report.getReportId();
      }
      if (result instanceof ChecklistRun run) {
        return run.getRunId();
      }
      if (result instanceof ChecklistTemplate template) {
        return template.getTemplateId();
      }
      if (result instanceof TemperatureLogEntry entry) {
        return entry.getEntryId();
      }
      if (result instanceof TrainingRecord record) {
        return record.getTrainingRecordId();
      }
      if (result instanceof Location location) {
        return location.getLocationId();
      }
      if (result instanceof AppUser user) {
        return user.getUserId();
      }
    } catch (Exception e) {
      log.debug("Could not extract entity ID from result", e);
    }

    return null;
  }

  private String getClientIpAddress() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
          return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
      }
    } catch (Exception e) {
      log.debug("Could not get client IP address", e);
    }
    return null;
  }

  private String getUserAgent() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("User-Agent");
      }
    } catch (Exception e) {
      log.debug("Could not get user agent", e);
    }
    return null;
  }

  private void logFailedAction(Integer orgNumber, Long userId, Audited audited,
                               ProceedingJoinPoint joinPoint, Throwable error) {
    try {
      String ipAddress = getClientIpAddress();
      String userAgent = getUserAgent();

      auditLogService.logAction(
          orgNumber,
          userId,
          audited.action(),
          audited.entityType(),
          null,
          null,
          "{\"error\":\"" + error.getMessage() + "\"}",
          ipAddress,
          userAgent);
    } catch (Exception e) {
      log.error("Error logging failed audit event", e);
    }
  }
}
