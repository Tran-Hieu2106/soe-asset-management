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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * SERVICE: AuditLogService
 * PURPOSE: The central engine handling read-access for auditors and write-access
 * for cross-module logging (Assets, Stock, Handover).
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Retrieves filtered, paginated logs for the RP-03 dashboard.
     * Transaction is read-only for performance.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(
            String module, String action, String performedBy, 
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        
        return auditLogRepository.searchLogs(module, action, performedBy, startDate, endDate, pageable)
                .map(AuditLogDto::from);
    }
    
    /**
     * The universal write method used by ALL other modules.
     * * RULE COMPLIANCE:
     * Uses Propagation.REQUIRED. This means if HandoverService calls this, it joins 
     * the Handover database transaction. If the Handover fails and rolls back, 
     * this log ALSO rolls back. This guarantees we don't log "phantom" events.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String module, String action, String recordId, String recordCode, 
                    String oldValue, String newValue, String description) {
        
        String username = "system";
        UUID userId = null;

        // Step 1: Extract real user from Spring Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            username = auth.getName();
            // Note: If you have a custom UserDetails object, you would cast it here to get the UUID.
            // For now, tracking username perfectly satisfies the audit requirement.
        }

        // Step 2: Extract real Client IP address securely
        String ipAddress = "unknown";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getHeader("X-Forwarded-For");
            
            // Fix: Proxies/Load balancers append IPs with commas (e.g., "client_ip, proxy_ip"). 
            // We only want the first one to prevent database column truncation crashes.
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        }

        // Step 3: Build and Persist
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