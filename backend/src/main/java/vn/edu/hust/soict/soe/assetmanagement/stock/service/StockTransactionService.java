package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.IssueRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.ReceiptRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockTransactionDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StorageLocation;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.InsufficientStockException;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StorageLocationRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StockTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // ── CS-02: RECEIPT ────────────────────────────────────────────────────
    public StockTransactionDto createReceipt(ReceiptRequest req, String createdBy) {
        log.info("Receipt — material: {}, location: {}, qty: {}",
                req.getMaterialId(), req.getStorageLocationId(), req.getQuantity());

        Material        material = findMaterialOrThrow(req.getMaterialId());
        StorageLocation location = findLocationOrThrow(req.getStorageLocationId());

        StockTransaction tx = StockTransaction.builder()
                .material(material)
                .storageLocation(location)
                .transactionType(TransactionType.RECEIPT)
                .quantity(req.getQuantity())
                .unitOfMeasure(material.getUnitOfMeasure())
                .unitPrice(req.getUnitPrice())
                .documentRef(req.getDocumentRef())
                .documentDate(req.getDocumentDate())
                .notes(req.getNotes())
                .approvedBy(req.getApprovedBy())
                .approvedAt(req.getApprovedBy() != null ? LocalDateTime.now() : null)
                .createdBy(createdBy)
                .build();

        return toDto(transactionRepository.save(tx));
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

        if (available.compareTo(req.getQuantity()) < 0) {
            log.warn("Insufficient stock — required: {}, available: {}",
                    req.getQuantity(), available);
            throw new InsufficientStockException(
                    "Tồn kho không đủ cho vật tư '" + material.getMaterialCode()
                    + "'. Cần: " + req.getQuantity() + ", Có: " + available,
                    req.getQuantity(), available);
        }

        StockTransaction tx = StockTransaction.builder()
                .material(material)
                .storageLocation(location)
                .transactionType(TransactionType.ISSUE)
                .quantity(req.getQuantity())
                .unitOfMeasure(material.getUnitOfMeasure())
                .unitPrice(req.getUnitPrice())
                .requestingDepartmentId(req.getRequestingDepartmentId())
                .requestedBy(req.getRequestedBy())
                .documentRef(req.getDocumentRef())
                .documentDate(req.getDocumentDate())
                .notes(req.getNotes())
                .approvedBy(req.getApprovedBy())
                .approvedAt(req.getApprovedBy() != null ? LocalDateTime.now() : null)
                .createdBy(createdBy)
                .build();

        return toDto(transactionRepository.save(tx));
    }

    // ── READ ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<StockTransactionDto> getByMaterial(UUID materialId) {
        return transactionRepository
                .findByMaterialIdOrderByDocumentDateDesc(materialId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Material findMaterialOrThrow(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vật tư ID " + id + " không tồn tại"));
    }

    private StorageLocation findLocationOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kho ID " + id + " không tồn tại"));
    }

    public StockTransactionDto toDto(StockTransaction t) {
        return StockTransactionDto.builder()
                .id(t.getId())
                .materialId(t.getMaterial().getId())
                .materialCode(t.getMaterial().getMaterialCode())
                .materialName(t.getMaterial().getName())
                .storageLocationId(t.getStorageLocation().getId())
                .storageLocationName(t.getStorageLocation().getName())
                .transactionType(t.getTransactionType())
                .quantity(t.getQuantity())
                .unitOfMeasure(t.getUnitOfMeasure())
                .unitPrice(t.getUnitPrice())
                .totalValue(t.getTotalValue())
                .requestingDepartmentId(t.getRequestingDepartmentId())
                .requestedBy(t.getRequestedBy())
                .documentRef(t.getDocumentRef())
                .documentDate(t.getDocumentDate())
                .notes(t.getNotes())
                .approvedBy(t.getApprovedBy())
                .approvedAt(t.getApprovedAt())
                .createdAt(t.getCreatedAt())
                .createdBy(t.getCreatedBy())
                .build();
    }
}
