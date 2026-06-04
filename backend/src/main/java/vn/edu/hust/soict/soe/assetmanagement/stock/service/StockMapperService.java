package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockTransactionDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.IssueRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.ReceiptRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StorageLocation;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class StockMapperService {

    /**
     * This service centralizes all mapping logic between entities and DTOs for the stock module.
     * It is injected into other services to avoid code duplication and ensure consistent mapping.
     */
    public StockTransaction toReceiptEntity(ReceiptRequest req, Material material, StorageLocation location, String createdBy) {
        if (req == null) return null;
        
        return StockTransaction.builder()
                .material(material)
                .storageLocation(location)
                .transactionType(TransactionType.RECEIPT) // Default to RECEIPT, can be overridden by caller if needed
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
    }

    /**
     * Maps an IssueRequest DTO to a StockTransaction entity for an ISSUE transaction.
     * The caller is responsible for validating stock availability before calling this method.
     *
     * @param req The IssueRequest containing the issue details.
     * @param material The Material entity being issued.
     * @param location The StorageLocation entity from which the material is issued.
     * @param finalUnitPrice The unit price to be used for this transaction, which may be based on current stock valuation.
     * @param createdBy The username of the user creating this transaction.
     * @return A StockTransaction entity ready for persistence.
     */
    public StockTransaction toIssueEntity(IssueRequest req, Material material, StorageLocation location, BigDecimal finalUnitPrice, String createdBy) {
        if (req == null) return null;
        
        return StockTransaction.builder()
                .material(material)
                .storageLocation(location)
                .transactionType(TransactionType.ISSUE) // Default to ISSUE, can be overridden by caller if needed
                .quantity(req.getQuantity())
                .unitOfMeasure(material.getUnitOfMeasure())
                .unitPrice(finalUnitPrice)
                .requestingDepartmentId(req.getRequestingDepartmentId())
                .requestedBy(req.getRequestedBy())
                .documentRef(req.getDocumentRef())
                .documentDate(req.getDocumentDate())
                .notes(req.getNotes())
                .approvedBy(req.getApprovedBy())
                .approvedAt(req.getApprovedBy() != null ? LocalDateTime.now() : null)
                .createdBy(createdBy)
                .build();
    }

    /** ── MATERIAL MAPPING ────────────────────────────────────────────────────────────
     * These methods map between Material entities and MaterialDto, as well as mapping from CreateMaterialRequest to Material.
     * This centralizes all material-related mapping logic in one place.
     */
    public StockTransactionDto toDto(StockTransaction t) {
        if (t == null) return null;
        
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

    /**
     * Maps a CreateMaterialRequest DTO to a Material entity.
     *
     * @param req The CreateMaterialRequest containing the material details.
     * @param category The MaterialCategory entity associated with the material.
     * @return A Material entity ready for persistence.
     */
    public Material toEntity(CreateMaterialRequest req, MaterialCategory category) {
        if (req == null) {
            return null;
        }

        return Material.builder()
                .materialCode(req.getMaterialCode())
                .name(req.getName())
                .category(category)
                .unitOfMeasure(req.getUnitOfMeasure())
                .technicalSpecs(req.getTechnicalSpecs())
                .supplierName(req.getSupplierName())
                .supplierCode(req.getSupplierCode())
                .unitPrice(req.getUnitPrice())
                .minimumStock(req.getMinimumStock())
                .notes(req.getNotes())
                .isActive(true) // New materials are active by default
                .build();
    }

    /**
     * Maps a Material entity to a MaterialDto.
     *
     * @param m The Material entity to be mapped.
     * @return A MaterialDto containing the relevant information.
     */
    public MaterialDto toDto(Material m) {
        if (m == null) {
            return null;
        }

        return MaterialDto.builder()
                .id(m.getId())
                .materialCode(m.getMaterialCode())
                .name(m.getName())
                .categoryId(m.getCategory() != null ? m.getCategory().getId() : null)
                .categoryName(m.getCategory() != null ? m.getCategory().getName() : null)
                .unitOfMeasure(m.getUnitOfMeasure())
                .technicalSpecs(m.getTechnicalSpecs())
                .supplierName(m.getSupplierName())
                .supplierCode(m.getSupplierCode())
                .unitPrice(m.getUnitPrice())
                .minimumStock(m.getMinimumStock())
                .isActive(m.getIsActive())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .createdBy(m.getCreatedBy())
                .build();
    }
}