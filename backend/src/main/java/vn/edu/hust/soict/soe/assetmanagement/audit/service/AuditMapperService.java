package vn.edu.hust.soict.soe.assetmanagement.audit.service;

import org.springframework.stereotype.Component;
import vn.edu.hust.soict.soe.assetmanagement.audit.dto.AuditLogDto;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;

@Component
public class AuditMapperService {

    public AuditLogDto toDto(AuditLog log) {
        if (log == null) return null;
        
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