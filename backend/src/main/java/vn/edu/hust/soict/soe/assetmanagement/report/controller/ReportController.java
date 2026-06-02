package vn.edu.hust.soict.soe.assetmanagement.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.StockReportDto;
import vn.edu.hust.soict.soe.assetmanagement.report.service.AssetReportService;
import vn.edu.hust.soict.soe.assetmanagement.report.service.ExportService;
import vn.edu.hust.soict.soe.assetmanagement.report.service.StockReportService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Asset inventory report (paginated, filterable)")
    public ResponseEntity<ApiResponse<PageResponse<AssetReportDto>>> getAssetReport(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) UUID managingUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionTo,
            Pageable pageable) {

        var page = assetReportService.generateAssetReport(
                status, categoryId, managingUnitId, acquisitionFrom, acquisitionTo, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Stock balance report for a date range")
    public ResponseEntity<ApiResponse<List<StockReportDto>>> getStockReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(stockReportService.generateStockReport(startDate, endDate)));
    }

    @GetMapping("/assets/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Export asset report")
    public ResponseEntity<byte[]> exportAssetReport(
            @RequestParam(defaultValue = "EXCEL") String format,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) UUID managingUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquisitionTo) {

        List<AssetReportDto> data = assetReportService.generateFullAssetRegister(
                status, categoryId, managingUnitId, acquisitionFrom, acquisitionTo);

        return switch (format.toUpperCase()) {
            case "PDF" -> binaryResponse(exportService.exportAssetsToPdf(data),
                    "application/pdf", "asset-report.pdf");
            case "CSV" -> binaryResponse(exportService.exportAssetsToCsv(data),
                    "text/csv; charset=UTF-8", "asset-report.csv");
            default -> binaryResponse(exportService.exportAssetsToExcel(data),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "asset-report.xlsx");
        };
    }

    @GetMapping("/stock/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT', 'APPROVING_AUTH')")
    @Operation(summary = "Export stock report")
    public ResponseEntity<byte[]> exportStockReport(
            @RequestParam(defaultValue = "EXCEL") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<StockReportDto> data = stockReportService.generateStockReport(startDate, endDate);
        byte[] body = exportService.exportStockReportToExcel(data);
        return binaryResponse(body,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "stock-report.xlsx");
    }

    private ResponseEntity<byte[]> binaryResponse(byte[] body, String contentType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
