package vn.edu.hust.soict.soe.assetmanagement.liquidation.controller;

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
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.CreateLiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.LiquidationDto;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.service.LiquidationService;

import java.util.Map;
import java.util.UUID;

/**
 * Liquidation endpoints (HL-02).
 */
@RestController
@RequestMapping("/api/liquidations")
@RequiredArgsConstructor
@Tag(name = "Liquidation", description = "Asset Liquidation Workflow")
@SecurityRequirement(name = "bearerAuth")
public class LiquidationController {

    private final LiquidationService liquidationService;

    @PostMapping
    @Operation(summary = "Create liquidation request", description = "Initiates a new asset disposal request")
    public ResponseEntity<ApiResponse<LiquidationDto>> createRequest(
            @Valid @RequestBody CreateLiquidationRequest request,
            Authentication authentication) {
        
        LiquidationDto dto = liquidationService.createRequest(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Liquidation request created successfully.", dto));
    }

    @PutMapping("/{id}/approve")
    // Aligned with SecurityConfig.java
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Approve liquidation request", description = "Approves request and sets asset status to LIQUIDATED (M2 dependency)")
    public ResponseEntity<ApiResponse<LiquidationDto>> approveRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        
        LiquidationDto dto = liquidationService.approveRequest(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Liquidation request approved successfully.", dto));
    }

    @PutMapping("/{id}/reject")
    // Aligned with SecurityConfig.java
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Reject liquidation request", description = "Rejects a pending request with a reason")
    public ResponseEntity<ApiResponse<LiquidationDto>> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication authentication) {
        
        String reason = (payload != null && payload.containsKey("reason")) ? payload.get("reason") : "No reason provided";
        LiquidationDto dto = liquidationService.rejectRequest(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.success("Liquidation request rejected successfully.", dto));
    }
}