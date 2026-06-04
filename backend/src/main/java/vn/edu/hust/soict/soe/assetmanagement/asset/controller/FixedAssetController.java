package vn.edu.hust.soict.soe.assetmanagement.asset.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.AssetHistoryDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Fixed Assets", description = "Quản lý vòng đời tài sản (FA-01 -> FA-04)")
@SecurityRequirement(name = "bearerAuth")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Lấy danh sách tài sản (phân trang, lọc)")
    public ResponseEntity<ApiResponse<PageResponse<FixedAssetDTO>>> getAllAssets(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) UUID managingUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionTo,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Page<FixedAssetDTO> dtoPage = fixedAssetService.searchAssets(
                status, categoryId, managingUnitId, acquisitionFrom, acquisitionTo, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tải danh sách thành công", PageResponse.of(dtoPage)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Xem chi tiết tài sản (tính khấu hao realtime)")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> getAssetById(@PathVariable UUID id) {
        FixedAssetDTO dto = fixedAssetService.calculateCurrentDepreciation(id);
        return ResponseEntity.ok(ApiResponse.success("Tải thông tin chi tiết thành công", dto));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Tạo mới hồ sơ tài sản")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> createAsset(
            @Valid @RequestBody FixedAssetDTO dto,
            Authentication authentication) {
        FixedAssetDTO createdDto = fixedAssetService.createAsset(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài sản thành công", createdDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Cập nhật thông tin tài sản")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> updateAsset(
            @PathVariable UUID id,
            @RequestBody FixedAssetDTO dto,
            Authentication authentication) {
        FixedAssetDTO updatedDto = fixedAssetService.updateAsset(id, dto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài sản thành công", updatedDto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Cập nhật trạng thái tài sản thủ công")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> updateStatus(
            @PathVariable UUID id,
            @RequestParam AssetStatus newStatus,
            @RequestParam String reason,
            Authentication authentication) {
        FixedAssetDTO updatedDto = fixedAssetService.updateAssetStatus(id, newStatus, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updatedDto));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Xem lịch sử vòng đời tài sản (FA-04)")
    public ResponseEntity<ApiResponse<List<AssetHistoryDTO>>> getAssetHistory(@PathVariable UUID id) {
        List<AssetHistoryDTO> history = fixedAssetService.getAssetHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Tải lịch sử thành công", history));
    }
}