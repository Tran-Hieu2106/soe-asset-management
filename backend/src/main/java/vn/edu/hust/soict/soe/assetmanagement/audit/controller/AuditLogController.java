package vn.edu.hust.soict.soe.assetmanagement.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.audit.dto.AuditLogDto;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ==============================================================================
 * CONTROLLER: AuditLogController
 * PURPOSE: Exposes the RP-03 Audit Log search API to the frontend.
 * RULE COMPLIANCE: 
 * - Strictly protected by RBAC (SYSTEM_ADMIN, FINANCE_AUDIT only).
 * - Implements the full suite of filters (module, action, user, date range).
 * - Wraps returns in ApiResponse<PageResponse<T>>.
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "System-wide immutable security logs (RP-03)")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT')")
    @Operation(summary = "Search and list audit logs", description = "Filterable by module, action, user, and date range.")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Ensure newest logs appear first
        Pageable pageable = PageRequest.of(page, size, Sort.by("performedAt").descending());
        
        // Convert LocalDate to LocalDateTime for precise database querying
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Page<AuditLogDto> logPage = auditLogService.getAuditLogs(
                module, action, performedBy, startDateTime, endDateTime, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Audit logs retrieved successfully", 
                PageResponse.of(logPage)
        ));
    }
}