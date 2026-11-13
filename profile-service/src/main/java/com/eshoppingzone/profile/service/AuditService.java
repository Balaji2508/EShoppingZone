package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.AuditRequest;
import com.eshoppingzone.profile.dto.AuditResponse;

import java.util.List;

public interface AuditService {
    AuditResponse recordAuditLog(AuditRequest request);
    AuditResponse recordAuditLog(String action, String performedBy, String entityType, String entityId, String details);
    List<AuditResponse> getAllAuditLogs();
}
