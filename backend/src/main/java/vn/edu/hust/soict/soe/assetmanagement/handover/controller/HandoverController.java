package vn.edu.hust.soict.soe.assetmanagement.handover.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.service.HandoverService;

import java.util.Map;
import java.util.UUID;

/**
 * Handover endpoints (HL-01).
 */
@RestController
@RequestMapping("/api/handovers")
@RequiredArgsConstructor
@Tag(name = "Handover", description = "Asset Handover Workflow")
@SecurityRequirement(name = "bearerAuth")
public class HandoverController {

    private final HandoverService handoverService;

    @PostMapping
    @Operation(summary = "Create handover request", description = "Initiates a new asset transfer request")
    public ResponseEntity<ApiResponse<HandoverDto>> createRequest(
            @Valid @RequestBody CreateHandoverRequest request,
            Authentication authentication) {
        
        HandoverDto dto = handoverService.createRequest(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Handover request created successfully.", dto));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve handover request", description = "Approves request and updates asset unit (M2 dependency)")
    public ResponseEntity<ApiResponse<HandoverDto>> approveRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        
        HandoverDto dto = handoverService.approveRequest(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Handover request approved successfully.", dto));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reject handover request", description = "Rejects a pending request with a reason")
    public ResponseEntity<ApiResponse<HandoverDto>> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication authentication) {
        
        String reason = (payload != null) ? payload.get("reason") : "No reason provided";
        HandoverDto dto = handoverService.rejectRequest(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.success("Handover request rejected successfully.", dto));
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Get Handover Document", description = "Returns enriched DTO for printing (HL-03)")
    public ResponseEntity<ApiResponse<HandoverDto>> getDocument(@PathVariable UUID id) {
        
        return ResponseEntity.ok(ApiResponse.success("Document data retrieved.", 
                handoverService.getDocumentData(id)));
    }
}