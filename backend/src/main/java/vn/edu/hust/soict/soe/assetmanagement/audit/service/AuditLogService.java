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
     * Write an audit log entry. 
     * Uses Propagation.REQUIRED: it joins the existing transaction of the calling service 
     * (e.g., FixedAssetService). If the main transaction fails and rolls back, the log 
     * rolls back too, preventing "phantom" logs of failed actions.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String module, String action, String recordId, String recordCode, 
                    String oldValue, String newValue, String description) {
        
        String username = "system";
        UUID userId = null;

        // 1. Extract current user from Spring Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            username = currentUser.getUsername();
            userId = currentUser.getId();
        }

        // 2. Extract Client IP securely
        String ipAddress = "unknown";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getHeader("X-Forwarded-For");
            
            // FIX: Proxies append IPs with commas (e.g., "client_ip, proxy_ip"). We only want the first one.
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        }

        // 3. Build and save
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
         
        auditLogRepository.save(auditLog);
        log.info("Audit log written: [{}] {} - {}", module, action, description);
    }
}