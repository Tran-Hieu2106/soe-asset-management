package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.IssueRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.ReceiptRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockTransactionDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StorageLocation;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StorageLocationRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StockTransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CS-02: Process receipts and issues; validate stock before issuing.
 * CS-04: Departmental allocation is recorded via requestingDepartmentId on ISSUE.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final MaterialRepository         materialRepository;
    private final StorageLocationRepository  locationRepository;
    private final AuditLogService            auditLogService;
    private final ObjectMapper               objectMapper;
    private final StockMapperService         stockMapperService; // Đã thêm Mapper Service

    // ── CS-02: RECEIPT ────────────────────────────────────────────────────
    public StockTransactionDto createReceipt(ReceiptRequest req, String createdBy) {
        log.info("Receipt — material: {}, location: {}, qty: {}",
                req.getMaterialId(), req.getStorageLocationId(), req.getQuantity());

        Material        material = findMaterialOrThrow(req.getMaterialId());
        StorageLocation location = findLocationOrThrow(req.getStorageLocationId());

        // Use Mapper to create entity
        StockTransaction tx = stockMapperService.toReceiptEntity(req, material, location, createdBy);

        StockTransaction savedTx = transactionRepository.save(tx);
        StockTransactionDto savedDto = stockMapperService.toDto(savedTx);
        
        // Use DTO for logging
        auditLogService.log("STOCK", "RECEIPT", savedTx.getId().toString(), savedTx.getDocumentRef(), 
                "{}", toJson(savedDto), "Recorded stock receipt for material: " + material.getMaterialCode());

        return savedDto;
    }

    // ── CS-02 + CS-04: ISSUE ──────────────────────────────────────────────
    public StockTransactionDto createIssue(IssueRequest req, String createdBy) {
        log.info("Issue — material: {}, dept: {}, qty: {}",
                req.getMaterialId(), req.getRequestingDepartmentId(), req.getQuantity());

        Material        material = findMaterialOrThrow(req.getMaterialId());
        StorageLocation location = findLocationOrThrow(req.getStorageLocationId());

        // Guard: cannot issue more than available stock
        BigDecimal available = transactionRepository
                .checkAvailableStock(req.getMaterialId(), req.getStorageLocationId());
        if (available == null) {
            available = BigDecimal.ZERO;
        }

        BigDecimal finalUnitPrice = req.getUnitPrice() != null 
                            ? req.getUnitPrice() 
                            : material.getUnitPrice();

        if (available.compareTo(req.getQuantity()) < 0) {
            log.warn("Insufficient stock — required: {}, available: {}", req.getQuantity(), available);
            throw new BusinessRuleException(
                    "Tồn kho không đủ cho vật tư '" + material.getMaterialCode()
                    + "'. Cần: " + req.getQuantity() + ", Có: " + available);
        }

        // Use Mapper to create entity
        StockTransaction tx = stockMapperService.toIssueEntity(req, material, location, finalUnitPrice, createdBy);

        StockTransaction savedTx = transactionRepository.save(tx);
        StockTransactionDto savedDto = stockMapperService.toDto(savedTx);
        
        // Use DTO for logging
        auditLogService.log("STOCK", "ISSUE", savedTx.getId().toString(), savedTx.getDocumentRef(), 
                "{}", toJson(savedDto), "Recorded stock issue to department: " + req.getRequestingDepartmentId());

        return savedDto;
    }

    // ── READ ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<StockTransactionDto> getByMaterial(UUID materialId) {
        return transactionRepository
                .findByMaterialIdOrderByDocumentDateDesc(materialId)
                .stream().map(stockMapperService::toDto).collect(Collectors.toList());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Material findMaterialOrThrow(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vật tư ID " + id + " không tồn tại"));
    }

    private StorageLocation findLocationOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kho ID " + id + " không tồn tại"));
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON parse error", e);
            return "{}";
        }
    }
}