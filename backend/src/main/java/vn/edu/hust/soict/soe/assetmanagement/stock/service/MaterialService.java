package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.UpdateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.DuplicateMaterialCodeException;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.ResourceNotFoundException;
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

    // ── CREATE ────────────────────────────────────────────────────────────
    public MaterialDto create(CreateMaterialRequest req, String createdBy) {
        log.info("Creating material: {}", req.getMaterialCode());

        if (materialRepository.existsByMaterialCode(req.getMaterialCode())) {
            throw new DuplicateMaterialCodeException(
                    "Mã vật tư '" + req.getMaterialCode() + "' đã tồn tại");
        }

        MaterialCategory category = findCategoryOrThrow(req.getCategoryId());

        Material m = Material.builder()
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
                .isActive(true)
                .createdBy(createdBy)
                .build();

        return toDto(materialRepository.save(m));
    }

    // ── READ ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<MaterialDto> getAll(Pageable pageable) {
        return materialRepository.findByIsActiveTrue(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MaterialDto> getByCategory(Integer categoryId, Pageable pageable) {
        return materialRepository
                .findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> search(String keyword) {
        return materialRepository.searchByName(keyword)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialDto getById(UUID id) {
        return toDto(findMaterialOrThrow(id));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public MaterialDto update(UUID id, UpdateMaterialRequest req) {
        log.info("Updating material ID: {}", id);
        Material m = findMaterialOrThrow(id);

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

        return toDto(materialRepository.save(m));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Material findMaterialOrThrow(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vật tư ID " + id + " không tồn tại"));
    }

    private MaterialCategory findCategoryOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Danh mục ID " + id + " không tồn tại"));
    }

    public MaterialDto toDto(Material m) {
        return MaterialDto.builder()
                .id(m.getId())
                .materialCode(m.getMaterialCode())
                .name(m.getName())
                .categoryId(m.getCategory().getId())
                .categoryName(m.getCategory().getName())
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
