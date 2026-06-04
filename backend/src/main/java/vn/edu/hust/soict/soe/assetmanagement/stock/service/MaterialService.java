package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.UpdateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialCategoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CS-01: Material catalogue — create, update, search
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository         materialRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final AuditLogService            auditLogService;
    private final ObjectMapper               objectMapper;
    private final StockMapperService         stockMapperService;

    // ── CREATE ────────────────────────────────────────────────────────────
    public MaterialDto create(CreateMaterialRequest req, String createdBy) {
        log.info("Creating material: {}", req.getMaterialCode());

        if (materialRepository.existsByMaterialCode(req.getMaterialCode())) {
            throw new BusinessRuleException("Mã vật tư '" + req.getMaterialCode() + "' đã tồn tại");
        }

        MaterialCategory category = findCategoryOrThrow(req.getCategoryId());
        Material m = stockMapperService.toEntity(req, category);

        // Save to DB
        Material saved = materialRepository.save(m);
        MaterialDto savedDto = stockMapperService.toDto(saved);
        auditLogService.log("STOCK", "CREATE_MATERIAL", saved.getId().toString(), saved.getMaterialCode(), 
                "{}", toJson(savedDto), "Registered new material");

        return savedDto;
    }

    // ── READ ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<MaterialDto> getAll(Pageable pageable) {
        return materialRepository.findByIsActiveTrue(pageable).map(stockMapperService::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MaterialDto> getByCategory(Integer categoryId, Pageable pageable) {
        return materialRepository
                .findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                .map(stockMapperService::toDto);
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> search(String keyword) {
        return materialRepository.searchByName(keyword)
                .stream()
                .map(stockMapperService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialDto getById(UUID id) {
        return stockMapperService.toDto(findMaterialOrThrow(id));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public MaterialDto update(UUID id, UpdateMaterialRequest req, String updatedBy) {
        log.info("Updating material ID: {}", id);
        Material m = findMaterialOrThrow(id);
        String oldJson = toJson(stockMapperService.toDto(m));

        if (req.getName()          != null) m.setName(req.getName());
        if (req.getUnitOfMeasure() != null) m.setUnitOfMeasure(req.getUnitOfMeasure());
        if (req.getTechnicalSpecs()!= null) m.setTechnicalSpecs(req.getTechnicalSpecs());
        if (req.getSupplierName()  != null) m.setSupplierName(req.getSupplierName());
        if (req.getSupplierCode()  != null) m.setSupplierCode(req.getSupplierCode());
        if (req.getUnitPrice()     != null) m.setUnitPrice(req.getUnitPrice());
        if (req.getMinimumStock()  != null) m.setMinimumStock(req.getMinimumStock());
        if (req.getIsActive()      != null) m.setIsActive(req.getIsActive());
        if (req.getNotes()         != null) m.setNotes(req.getNotes());
        if (req.getCategoryId()    != null) m.setCategory(findCategoryOrThrow(req.getCategoryId()));

        Material updated = materialRepository.save(m);
        MaterialDto savedDto = stockMapperService.toDto(updated);
        
        auditLogService.log("STOCK", "UPDATE_MATERIAL", updated.getId().toString(), updated.getMaterialCode(), 
                oldJson, toJson(savedDto), "Updated material properties");

        return savedDto;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Material findMaterialOrThrow(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vật tư ID " + id + " không tồn tại"));
    }

    private MaterialCategory findCategoryOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục ID " + id + " không tồn tại"));
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