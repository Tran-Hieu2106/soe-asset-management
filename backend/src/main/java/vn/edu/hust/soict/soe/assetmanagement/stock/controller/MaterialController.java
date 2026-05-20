package vn.edu.hust.soict.soe.assetmanagement.stock.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.UpdateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.MaterialService;

import java.util.List;
import java.util.UUID;

/**
 * CS-01: Material catalogue REST API
 * Refactored to use global ApiResponse, PageResponse, and RBAC security.
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<PageResponse<MaterialDto>>> getAll(
            @RequestParam(required = false) Integer categoryId,
            Pageable pageable) {

        Page<MaterialDto> page = categoryId != null
                ? materialService.getByCategory(categoryId, pageable)
                : materialService.getAll(pageable);
                
        return ResponseEntity.ok(ApiResponse.success("Materials retrieved", PageResponse.of(page)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<List<MaterialDto>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(materialService.search(keyword)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<MaterialDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<MaterialDto>> create(
            @Valid @RequestBody CreateMaterialRequest req, 
            Authentication authentication) {
        
        // Extracted username from security context instead of hardcoding "system"
        MaterialDto created = materialService.create(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<MaterialDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaterialRequest req,
            Authentication authentication) {

        MaterialDto updated = materialService.update(id, req, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Material updated successfully", updated));
    }
}