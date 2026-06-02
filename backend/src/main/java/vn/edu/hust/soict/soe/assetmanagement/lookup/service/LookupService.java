package vn.edu.hust.soict.soe.assetmanagement.lookup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetCategory;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetCategoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.lookup.dto.LookupItemDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StorageLocation;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialCategoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StorageLocationRepository;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.ManagingUnit;
import vn.edu.hust.soict.soe.assetmanagement.user.repository.ManagingUnitRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupService {

    private final ManagingUnitRepository managingUnitRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final MaterialCategoryRepository materialCategoryRepository;
    private final StorageLocationRepository storageLocationRepository;

    public List<LookupItemDto> getManagingUnits() {
        return managingUnitRepository.findByIsActiveTrueOrderByCodeAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<LookupItemDto> getAssetCategories() {
        return assetCategoryRepository.findAll().stream()
                .map(c -> LookupItemDto.builder()
                        .id(String.valueOf(c.getId()))
                        .code(c.getCode())
                        .name(c.getName())
                        .build())
                .toList();
    }

    public List<LookupItemDto> getMaterialCategories() {
        return materialCategoryRepository.findAll().stream()
                .map(c -> LookupItemDto.builder()
                        .id(String.valueOf(c.getId()))
                        .code(c.getCode())
                        .name(c.getName())
                        .build())
                .toList();
    }

    public List<LookupItemDto> getStorageLocations() {
        return storageLocationRepository.findByIsActiveTrue().stream()
                .map(l -> LookupItemDto.builder()
                        .id(l.getId().toString())
                        .code(l.getCode())
                        .name(l.getName())
                        .build())
                .toList();
    }

    public Optional<ManagingUnit> findUnit(UUID id) {
        return managingUnitRepository.findById(id);
    }

    public Optional<AssetCategory> findAssetCategory(Integer id) {
        return assetCategoryRepository.findById(id);
    }

    public Optional<ManagingUnit> findUnitByCode(String code) {
        return managingUnitRepository.findByCode(code);
    }

    public Optional<AssetCategory> findAssetCategoryByCode(String code) {
        return assetCategoryRepository.findByCode(code);
    }

    private LookupItemDto toDto(ManagingUnit unit) {
        return LookupItemDto.builder()
                .id(unit.getId().toString())
                .code(unit.getCode())
                .name(unit.getName())
                .build();
    }
}
