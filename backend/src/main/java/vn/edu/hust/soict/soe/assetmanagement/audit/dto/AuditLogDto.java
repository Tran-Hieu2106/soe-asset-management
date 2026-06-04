package vn.edu.hust.soict.soe.assetmanagement.audit.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * DTO: AuditLogDto
 * PURPOSE: Secure payload object to send audit trail data to the frontend.
 * RULE COMPLIANCE: Adheres to the "Never expose JPA entity objects directly" rule.
 * ==============================================================================
 */
@Getter
@Builder
public class AuditLogDto {
    private UUID id;
    private String module;
    private String action;
    private String recordId;
    private String recordCode;
    private String performedBy;
    private String ipAddress;
    private String oldValue;
    private String newValue;
    private String description;
    private LocalDateTime performedAt;
}