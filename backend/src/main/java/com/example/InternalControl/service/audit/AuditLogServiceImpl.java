package com.example.InternalControl.service.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.audit.AuditLogRepository;
import com.example.InternalControl.repository.user.AppUserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(Integer orgNumber, Long userId, ActionType actionType,
                             String entityType, Long entityId,
                             String oldValuesJson, String newValuesJson,
                             String ipAddress, String userAgent) {

        AppUser actedByUser = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        AuditLog auditLog = new AuditLog();
        auditLog.setOrgNumber(orgNumber);
        auditLog.setActedByUser(actedByUser);
        auditLog.setActionType(actionType != null ? actionType.name() : null);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValuesJson(oldValuesJson);
        auditLog.setNewValuesJson(newValuesJson);
        auditLog.setUserAgent(userAgent);

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} on {} id={}", actionType, entityType, entityId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByOrganization(Integer orgNumber) {
        return auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(Integer orgNumber, LocalDateTime fromDate, LocalDateTime toDate) {
        return auditLogRepository.findByOrgNumberAndCreatedAtAfter(orgNumber, fromDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByActionType(Integer orgNumber, ActionType actionType) {
        // For now, return all logs and filter by action type in memory
        // Or use a custom query
        return auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(orgNumber)
                .stream()
                .filter(log -> actionType.name().equals(log.getActionType()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return auditLogRepository.findByActedByUserOrderByCreatedAtDesc(user);
    }
}
