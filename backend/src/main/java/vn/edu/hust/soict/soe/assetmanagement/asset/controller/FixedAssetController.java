package vn.edu.hust.soict.soe.assetmanagement.asset.controller;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.AssetHistoryDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse; // IMPORT ApiResponse

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fixed asset endpoints.
 * Refactored to exclusively return ApiResponse wrappers and DTOs.
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Fixed Assets", description = "Fixed Asset Management Module (FA-01 -> FA-04)")
public class FixedAssetController {

    @Autowired
    private FixedAssetService fixedAssetService;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Operation(summary = "Get all assets", description = "Returns a list of all digital asset profiles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedAssetDTO>>> getAllAssets() {
        List<FixedAssetDTO> dtos = fixedAssetService.getAllAssets().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Assets retrieved successfully", dtos));
    }

    @Operation(summary = "Get asset details", description = "Retrieve full technical parameters and current financial status by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> getAssetById(@PathVariable @NonNull UUID id) {
        FixedAsset asset = fixedAssetService.calculateCurrentDepreciation(id);
        return ResponseEntity.ok(ApiResponse.success(mapToDTO(asset)));
    }

    @Operation(summary = "Create new asset profile", description = "Create a new asset profile with full parameters (FA-01)")
    @PostMapping
    public ResponseEntity<ApiResponse<FixedAssetDTO>> createAsset(@Valid @RequestBody FixedAssetDTO assetDTO) {
        FixedAsset newAsset = fixedAssetService.createAsset(assetDTO);
        return new ResponseEntity<>(ApiResponse.success("Asset created successfully", mapToDTO(newAsset)), HttpStatus.CREATED);
    }

    @Operation(summary = "Calculate depreciation", description = "Calculate accumulated depreciation and remaining book value per Circular 45/2013/TT-BTC")
    @GetMapping("/{id}/depreciation")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> calculateDepreciation(@PathVariable @NonNull UUID id) {
        FixedAsset asset = fixedAssetService.calculateCurrentDepreciation(id);
        return ResponseEntity.ok(ApiResponse.success("Depreciation calculated", mapToDTO(asset)));
    }

    @Operation(summary = "Update operational status", description = "Change asset status (maintenance, liquidation...) and save history log (FA-03)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FixedAssetDTO>> updateStatus(
            @PathVariable @NonNull UUID id, 
            @RequestParam AssetStatus newStatus, 
            @RequestParam String reason) {
        
        FixedAsset updated = fixedAssetService.updateAssetStatus(id, newStatus, reason, "current_user"); 
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", mapToDTO(updated)));
    }

    @Operation(summary = "Get asset lifecycle history", description = "Retrieve immutable log of status changes and reassignments (FA-04)")
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AssetHistoryDTO>>> getAssetHistory(@PathVariable @NonNull UUID id) {
        List<AssetHistoryDTO> history = assetHistoryRepository.findByAssetIdOrderByPerformedAtDesc(id).stream()
                .map(this::mapToHistoryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Asset history retrieved", history));
    }

    // --- HELPER MAPPERS ---
    // Maps Entity to DTO to prevent exposing JPA Entity details to the frontend
    
    private FixedAssetDTO mapToDTO(FixedAsset asset) {
        FixedAssetDTO dto = new FixedAssetDTO();
        dto.setId(asset.getId());
        dto.setAssetCode(asset.getAssetCode());
        dto.setName(asset.getName());
        dto.setCategoryId(asset.getCategoryId());
        dto.setManagingUnitId(asset.getManagingUnitId());
        dto.setSerialNumber(asset.getSerialNumber());
        dto.setManufacturer(asset.getManufacturer());
        dto.setModel(asset.getModel());
        dto.setCountryOfOrigin(asset.getCountryOfOrigin());
        dto.setTechnicalSpecs(asset.getTechnicalSpecs());
        dto.setLocation(asset.getLocation());
        dto.setOriginalCost(asset.getOriginalCost());
        dto.setAcquisitionDate(asset.getAcquisitionDate());
        dto.setFundingSource(asset.getFundingSource());
        dto.setUsefulLifeYears(asset.getUsefulLifeYears());
        dto.setSalvageValue(asset.getSalvageValue());
        dto.setDepreciationMethod(asset.getDepreciationMethod());
        dto.setAccumulatedDepreciation(asset.getAccumulatedDepreciation());
        dto.setNetBookValue(asset.getNetBookValue());
        dto.setStatus(asset.getStatus());
        return dto;
    }

    private AssetHistoryDTO mapToHistoryDTO(AssetHistory history) {
        AssetHistoryDTO dto = new AssetHistoryDTO();
        dto.setId(history.getId());
        dto.setAssetId(history.getAssetId());
        dto.setEventType(history.getEventType());
        dto.setDescription(history.getDescription());
        dto.setOldValue(history.getOldValue());
        dto.setNewValue(history.getNewValue());
        dto.setPerformedBy(history.getPerformedBy());
        dto.setPerformedAt(history.getPerformedAt());
        return dto;
    }
}