package com.example.InternalControl.service.audit;

import java.time.LocalDateTime;
import java.util.List;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;

public interface AuditLogService {

    AuditLog logAction(Integer orgNumber, Long userId, ActionType actionType,
                      String entityType, Long entityId,
                      String oldValuesJson, String newValuesJson,
                      String ipAddress, String userAgent);

    List<AuditLog> getAuditLogsByOrganization(Integer orgNumber);

    List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId);

    List<AuditLog> getAuditLogsByDateRange(Integer orgNumber, LocalDateTime fromDate, LocalDateTime toDate);

    List<AuditLog> getAuditLogsByActionType(Integer orgNumber, ActionType actionType);

    List<AuditLog> getAuditLogsByUser(Long userId);
}
