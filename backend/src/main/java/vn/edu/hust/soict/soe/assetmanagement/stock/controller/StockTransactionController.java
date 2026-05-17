package vn.edu.hust.soict.soe.assetmanagement.stock.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.*;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.StockBalanceService;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.StockTransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CS-02 + CS-03 + CS-04: Stock operations REST API
 *
 * POST /api/stock/receipt                  — CS-02: record incoming stock
 * POST /api/stock/issue                    — CS-02 + CS-04: record stock issued to a dept
 * GET  /api/stock/balance                  — CS-03: balance for all materials
 * GET  /api/stock/balance/{materialId}     — CS-03: balance for one material
 * GET  /api/stock/usage                    — CS-04: dept-wise consumption summary
 * GET  /api/stock/transactions/{materialId}— history for a material
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService transactionService;
    private final StockBalanceService     balanceService;

    // ── CS-02 ─────────────────────────────────────────────────────────────

    @PostMapping("/receipt")
    public ResponseEntity<StockTransactionDto> receipt(
            @Valid @RequestBody ReceiptRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.createReceipt(req, "system")); // TODO: JWT
    }

    @PostMapping("/issue")
    public ResponseEntity<StockTransactionDto> issue(
            @Valid @RequestBody IssueRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.createIssue(req, "system")); // TODO: JWT
    }

    @GetMapping("/transactions/{materialId}")
    public ResponseEntity<List<StockTransactionDto>> transactionHistory(
            @PathVariable UUID materialId) {

        return ResponseEntity.ok(transactionService.getByMaterial(materialId));
    }

    // ── CS-03 ─────────────────────────────────────────────────────────────

    @GetMapping("/balance")
    public ResponseEntity<List<StockBalanceDto>> getAllBalances() {
        return ResponseEntity.ok(balanceService.getAllBalances());
    }

    @GetMapping("/balance/{materialId}")
    public ResponseEntity<List<StockBalanceDto>> getBalanceByMaterial(
            @PathVariable UUID materialId) {

        return ResponseEntity.ok(balanceService.getBalanceByMaterial(materialId));
    }

    // ── CS-04 ─────────────────────────────────────────────────────────────

    @GetMapping("/usage")
    public ResponseEntity<List<DepartmentUsageDto>> getDepartmentUsage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(balanceService.getDepartmentUsage(startDate, endDate));
    }
}
