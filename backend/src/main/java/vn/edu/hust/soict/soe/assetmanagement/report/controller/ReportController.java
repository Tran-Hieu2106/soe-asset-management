package vn.edu.hust.soict.soe.assetmanagement.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.StockReportDto;
import vn.edu.hust.soict.soe.assetmanagement.report.service.AssetReportService;
import vn.edu.hust.soict.soe.assetmanagement.report.service.ExportService;
import vn.edu.hust.soict.soe.assetmanagement.report.service.StockReportService;

import java.time.LocalDate;
import java.util.List;

/**
 * Report endpoints (RP-01, RP-02).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Management Reporting & Exports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final AssetReportService assetReportService;
    private final StockReportService stockReportService;
    private final ExportService exportService;

    @GetMapping("/assets")
    // Aligned with SecurityConfig.java
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Get full asset register", description = "Returns tabular data for all fixed assets")
    public ResponseEntity<ApiResponse<List<AssetReportDto>>> getAssetReport() {
        List<AssetReportDto> report = assetReportService.generateFullAssetRegister();
        return ResponseEntity.ok(ApiResponse.success("Asset report generated successfully", report));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Get stock balance report", description = "Returns stock movement and balances for a given date range")
    public ResponseEntity<ApiResponse<List<StockReportDto>>> getStockReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<StockReportDto> report = stockReportService.generateStockReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Stock report generated successfully", report));
    }

    @GetMapping("/assets/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Export asset report to CSV", description = "Downloads the asset register as a CSV file readable by Excel")
    public ResponseEntity<byte[]> exportAssetReport() {
        
        List<AssetReportDto> data = assetReportService.generateFullAssetRegister();
        byte[] csvFile = exportService.exportAssetsToCsv(data);

        // NOTE: We intentionally bypass ApiResponse here. 
        // File downloads must return raw binary data with specific headers so browsers trigger a "Save As" download.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "Asset_Register_Report.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvFile);
    }
}