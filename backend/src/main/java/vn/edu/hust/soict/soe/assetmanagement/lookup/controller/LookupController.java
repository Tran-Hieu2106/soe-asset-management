package vn.edu.hust.soict.soe.assetmanagement.lookup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.lookup.dto.LookupItemDto;
import vn.edu.hust.soict.soe.assetmanagement.lookup.service.LookupService;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
@Tag(name = "Lookups", description = "Reference data for forms and filters")
@SecurityRequirement(name = "bearerAuth")
public class LookupController {

    private final LookupService lookupService;

    @GetMapping("/managing-units")
    @Operation(summary = "List managing units")
    public ResponseEntity<ApiResponse<List<LookupItemDto>>> managingUnits() {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getManagingUnits()));
    }

    @GetMapping("/asset-categories")
    @Operation(summary = "List asset categories")
    public ResponseEntity<ApiResponse<List<LookupItemDto>>> assetCategories() {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getAssetCategories()));
    }

    @GetMapping("/material-categories")
    @Operation(summary = "List material categories")
    public ResponseEntity<ApiResponse<List<LookupItemDto>>> materialCategories() {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getMaterialCategories()));
    }

    @GetMapping("/storage-locations")
    @Operation(summary = "List storage locations")
    public ResponseEntity<ApiResponse<List<LookupItemDto>>> storageLocations() {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getStorageLocations()));
    }
}
