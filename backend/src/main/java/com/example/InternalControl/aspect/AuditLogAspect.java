package com.example.InternalControl.aspect;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Captures audit events by intercepting methods annotated with {@link Audited}.
 *
 * @author TriTacLe
 * @since 1.0
 */
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

  @AfterReturning(value = "@annotation(audited)", returning = "result")
  public void logAuditEvent(JoinPoint joinPoint, Audited audited, Object result) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        log.debug("No authenticated user, skipping audit log");
        return;
      }

      Long userId = extractUserId(authentication);
      if (userId == null) {
        log.debug("Could not extract user ID, skipping audit log");
        return;
      }

      Integer orgNumber = extractOrgNumber(joinPoint);
      Long entityId = extractEntityId(result, audited.entityId());

      String ipAddress = getClientIpAddress();
      String userAgent = getUserAgent();

      String newValuesJson = null;
      if (result != null && (audited.action() == ActionType.CREATE || audited.action() == ActionType.UPDATE)) {
        try {
          newValuesJson = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
          log.warn("Could not serialize result to JSON for audit log", e);
        }
      }

      auditLogService.logAction(
          orgNumber,
          userId,
          audited.action(),
          audited.entityType(),
          entityId,
          null,
          newValuesJson,
          ipAddress,
          userAgent);

    } catch (Exception e) {
      log.error("Error logging audit event", e);
    }
  }

  private Long extractUserId(Authentication authentication) {
    if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
      return userDetails.getUserId();
    }
    return null;
  }

  private Integer extractOrgNumber(JoinPoint joinPoint) {
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

  private Long extractEntityId(Object result, String entityIdExpression) {
    if (result == null || entityIdExpression.isEmpty()) {
      return null;
    }

    try {
      if (result instanceof com.example.InternalControl.model.deviation.DeviationReport report) {
        return report.getReportId();
      }
      if (result instanceof com.example.InternalControl.model.checklist.ChecklistRun run) {
        return run.getRunId();
      }
      if (result instanceof com.example.InternalControl.model.temperature.TemperatureLogEntry entry) {
        return entry.getEntryId();
      }
      if (result instanceof com.example.InternalControl.model.user.AppUser user) {
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
}
