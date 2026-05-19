package vn.edu.hust.soict.soe.assetmanagement.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.hust.soict.soe.assetmanagement.audit.dto.AuditLogDto;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;
import vn.edu.hust.soict.soe.assetmanagement.audit.repository.AuditLogRepository;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.User;

import java.util.UUID;
/**
 * Audit log service (RP-03).
 * Provides the centralized log() method used by all other modules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * RP-03: Read-only query for Audit Logs.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(String module, String action, Pageable pageable) {
        return auditLogRepository.searchLogs(module, action, pageable)
                .map(AuditLogDto::from);
    }
    
    /**
     * Write an audit log entry. This method should be called within the same transaction as the business logic 
     * so if the transaction rolls back, the log will not be saved either, ensuring consistency between logs and data state.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String module, String action, String recordId, String recordCode, 
                    String oldValue, String newValue, String description) {
        
        String username = "system";
        UUID userId = null;

        // Get current user info from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            username = currentUser.getUsername();
            userId = currentUser.getId();
        }

        // Get client IP address from request context
        String ipAddress = "unknown";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
        }

        // Build and save the audit log entry
        AuditLog auditLog = AuditLog.builder()
                .module(module)
                .action(action)
                .recordId(recordId)
                .recordCode(recordCode)
                .performedBy(username)
                .userId(userId)
                .ipAddress(ipAddress)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .build();
         
        if (auditLog != null) {
            auditLogRepository.save(auditLog);
            }
        log.info("Audit log written: [{}] {} - {}", module, action, description);
    }

}