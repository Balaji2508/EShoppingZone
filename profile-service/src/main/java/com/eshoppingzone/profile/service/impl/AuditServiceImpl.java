package com.eshoppingzone.profile.service.impl;

import com.eshoppingzone.profile.dto.AuditRequest;
import com.eshoppingzone.profile.dto.AuditResponse;
import com.eshoppingzone.profile.entity.AuditLog;
import com.eshoppingzone.profile.repository.AuditLogRepository;
import com.eshoppingzone.profile.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditResponse recordAuditLog(AuditRequest request) {
        return recordAuditLog(
                request.getAction(),
                request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM",
                request.getEntityType() != null ? request.getEntityType() : "GENERAL",
                request.getEntityId(),
                request.getDetails()
        );
    }

    @Override
    public AuditResponse recordAuditLog(String action, String performedBy, String entityType, String entityId, String details) {
        logger.info("[AUDIT] Action: {}, PerformedBy: {}, Target: {}:{}, Details: {}", action, performedBy, entityType, entityId, details);
        AuditLog auditLog = new AuditLog(
                action,
                performedBy != null ? performedBy : "SYSTEM",
                entityType != null ? entityType : "GENERAL",
                entityId,
                details,
                LocalDateTime.now()
        );
        AuditLog saved = auditLogRepository.save(auditLog);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditResponse> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AuditResponse mapToResponse(AuditLog log) {
        return new AuditResponse(
                log.getId(),
                log.getAction(),
                log.getPerformedBy(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getTimestamp()
        );
    }
}
