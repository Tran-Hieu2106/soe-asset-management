package vn.edu.hust.soict.soe.assetmanagement.asset.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.AssetHistoryDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * CONTROLLER: FixedAssetController
 * PURPOSE: Exposes REST API for Assets. 
 * RULE CHECK: 
 * - Wraps all returns in ApiResponse and PageResponse.
 * - Applies @PreAuthorize exactly as dictated in SecurityConfig.java.
 * - Extracts username securely from the Authentication object.
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Fixed Assets", description = "Quản lý vòng đời tài sản (FA-01 -> FA-04)")
@SecurityRequirement(name = "bearerAuth")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;
    private final AssetHistoryRepository assetHistoryRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Lấy danh sách tài sản (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<FixedAssetDTO>>> getAllAssets(Pageable pageable) {
        Page<FixedAsset> page = fixedAssetService.getAllAssets(pageable);
        Page<FixedAssetDTO> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(ApiResponse.success("Tải danh sách thành công", PageResponse.of(dtoPage)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Xem chi tiết tài sản (tính khấu hao realtime)")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> getAssetById(@PathVariable UUID id) {
        FixedAsset asset = fixedAssetService.calculateCurrentDepreciation(id);
        return ResponseEntity.ok(ApiResponse.success("Tải thông tin chi tiết thành công", mapToDto(asset)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Tạo mới hồ sơ tài sản")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> createAsset(
            @Valid @RequestBody FixedAssetDTO dto, 
            Authentication authentication) {
        
        FixedAsset created = fixedAssetService.createAsset(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài sản thành công", mapToDto(created)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Cập nhật trạng thái tài sản thủ công (ví dụ: Bảo trì)")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> updateStatus(
            @PathVariable UUID id, 
            @RequestParam AssetStatus newStatus, 
            @RequestParam String reason,
            Authentication authentication) {
        
        FixedAsset updated = fixedAssetService.updateAssetStatus(id, newStatus, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", mapToDto(updated)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Xem lịch sử vòng đời tài sản (FA-04)")
    public ResponseEntity<ApiResponse<List<AssetHistoryDTO>>> getAssetHistory(@PathVariable UUID id) {
        List<AssetHistoryDTO> history = assetHistoryRepository.findByAssetIdOrderByPerformedAtDesc(id)
                .stream().map(this::mapToHistoryDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Tải lịch sử thành công", history));
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────
    private FixedAssetDTO mapToDto(FixedAsset asset) {
        return FixedAssetDTO.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .name(asset.getName())
                .categoryId(asset.getCategoryId())
                .managingUnitId(asset.getManagingUnitId())
                .serialNumber(asset.getSerialNumber())
                .manufacturer(asset.getManufacturer())
                .model(asset.getModel())
                .countryOfOrigin(asset.getCountryOfOrigin())
                .technicalSpecs(asset.getTechnicalSpecs())
                .location(asset.getLocation())
                .originalCost(asset.getOriginalCost())
                .acquisitionDate(asset.getAcquisitionDate())
                .usefulLifeYears(asset.getUsefulLifeYears())
                .salvageValue(asset.getSalvageValue())
                .depreciationMethod(asset.getDepreciationMethod())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .netBookValue(asset.getNetBookValue())
                .status(asset.getStatus())
                .notes(asset.getNotes())
                .build();
    }

    private AssetHistoryDTO mapToHistoryDto(AssetHistory h) {
        return AssetHistoryDTO.builder()
                .id(h.getId())
                .assetId(h.getAssetId())
                .eventType(h.getEventType())
                .description(h.getDescription())
                .oldValue(h.getOldValue())
                .newValue(h.getNewValue())
                .performedBy(h.getPerformedBy())
                .performedAt(h.getPerformedAt())
                .build();
    }
}