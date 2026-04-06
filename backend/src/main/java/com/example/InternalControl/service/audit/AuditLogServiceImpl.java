package com.example.InternalControl.service.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(Integer orgNumber, Long userId, ActionType actionType,
                             String entityType, Long entityId,
                             String oldValuesJson, String newValuesJson,
                             String ipAddress, String userAgent) {

        AuditLog auditLog = AuditLog.builder()
                .orgNumber(orgNumber)
                .actedByUserId(userId)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .oldValuesJson(oldValuesJson)
                .newValuesJson(newValuesJson)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

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
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(Integer orgNumber, LocalDateTime fromDate, LocalDateTime toDate) {
        return auditLogRepository.findByOrgNumberAndDateRange(orgNumber, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByActionType(Integer orgNumber, ActionType actionType) {
        return auditLogRepository.findByOrgNumberAndActionType(orgNumber, actionType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }
}
