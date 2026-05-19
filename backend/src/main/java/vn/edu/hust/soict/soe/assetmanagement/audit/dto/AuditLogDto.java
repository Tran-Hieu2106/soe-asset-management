package vn.edu.hust.soict.soe.assetmanagement.audit.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Response body for audit log queries (RP-03).
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

    public static AuditLogDto from(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .module(log.getModule())
                .action(log.getAction())
                .recordId(log.getRecordId())
                .recordCode(log.getRecordCode())
                .performedBy(log.getPerformedBy())
                .ipAddress(log.getIpAddress())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .performedAt(log.getPerformedAt())
                .build();
    }
}