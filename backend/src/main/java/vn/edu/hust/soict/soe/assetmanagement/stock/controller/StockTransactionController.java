package vn.edu.hust.soict.soe.assetmanagement.stock.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.*;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.StockBalanceService;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.StockTransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CS-02 + CS-03 + CS-04: Stock operations REST API
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService transactionService;
    private final StockBalanceService     balanceService;

    // ── CS-02 ─────────────────────────────────────────────────────────────

    @PostMapping("/receipt")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<StockTransactionDto>> receipt(
            @Valid @RequestBody ReceiptRequest req,
            Authentication authentication) {

        StockTransactionDto dto = transactionService.createReceipt(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Receipt recorded successfully", dto));
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<StockTransactionDto>> issue(
            @Valid @RequestBody IssueRequest req,
            Authentication authentication) {

        StockTransactionDto dto = transactionService.createIssue(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Issue recorded successfully", dto));
    }

    @GetMapping("/transactions/{materialId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<List<StockTransactionDto>>> transactionHistory(
            @PathVariable UUID materialId) {

        return ResponseEntity.ok(ApiResponse.success(transactionService.getByMaterial(materialId)));
    }

    // ── CS-03 ─────────────────────────────────────────────────────────────

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<List<StockBalanceDto>>> getAllBalances() {
        return ResponseEntity.ok(ApiResponse.success(balanceService.getAllBalances()));
    }

    @GetMapping("/balance/{materialId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<List<StockBalanceDto>>> getBalanceByMaterial(
            @PathVariable UUID materialId) {

        return ResponseEntity.ok(ApiResponse.success(balanceService.getBalanceByMaterial(materialId)));
    }

    // ── CS-04 ─────────────────────────────────────────────────────────────

    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'WAREHOUSE', 'APPROVING_AUTH', 'FINANCE_AUDIT')")
    public ResponseEntity<ApiResponse<List<DepartmentUsageDto>>> getDepartmentUsage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(balanceService.getDepartmentUsage(startDate, endDate)));
    }
}