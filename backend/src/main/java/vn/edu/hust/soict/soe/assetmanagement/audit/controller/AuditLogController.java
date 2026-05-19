package vn.edu.hust.soict.soe.assetmanagement.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.audit.dto.AuditLogDto;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;
/**
 * Audit log endpoints (RP-03).
 * Handles GET /api/audit-logs for SYSTEM_ADMIN and FINANCE_AUDIT.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "SYSTEM_ADMIN and FINANCE_AUDIT only")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT')")
    @Operation(summary = "Search and list audit logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("performedAt").descending());
        Page<AuditLogDto> logPage = auditLogService.getAuditLogs(module, action, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(logPage)));
    }
}