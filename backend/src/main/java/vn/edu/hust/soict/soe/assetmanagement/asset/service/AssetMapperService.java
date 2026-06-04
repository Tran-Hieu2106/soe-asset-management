package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.AssetHistoryDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.lookup.service.LookupService;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
//import vn.edu.hust.soict.soe.assetmanagement.asset.enums.DepreciationMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AssetMapperService {

    private final LookupService lookupService;

    /**
     * Converts an AssetHistory JPA entity to its corresponding DTO.
     * This ensures the internal structure of AssetHistory is abstracted away from API consumers.
     */
    public AssetHistoryDTO toHistoryDto(AssetHistory history) {
        return AssetHistoryDTO.builder()
                .id(history.getId())
                .assetId(history.getAssetId())
                .eventType(history.getEventType())
                .description(history.getDescription())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .performedBy(history.getPerformedBy())
                .performedAt(history.getPerformedAt())
                .build();
    }

    public FixedAsset toEntity(FixedAssetDTO dto) {
        return FixedAsset.builder()
                .assetCode(dto.getAssetCode())
                .name(dto.getName())
                .categoryId(dto.getCategoryId())
                .managingUnitId(dto.getManagingUnitId())
                .serialNumber(dto.getSerialNumber())
                .manufacturer(dto.getManufacturer())
                .model(dto.getModel())
                .countryOfOrigin(dto.getCountryOfOrigin())
                .technicalSpecs(dto.getTechnicalSpecs())
                .location(dto.getLocation())
                .originalCost(dto.getOriginalCost())
                .acquisitionDate(dto.getAcquisitionDate())
                .usefulLifeYears(dto.getUsefulLifeYears())
                .salvageValue(dto.getSalvageValue() != null ? dto.getSalvageValue() : BigDecimal.ZERO)
                .accumulatedDepreciation(BigDecimal.ZERO)
                .netBookValue(dto.getOriginalCost())
                .status(AssetStatus.IN_USE)
                .depreciationMethod(dto.getDepreciationMethod())
                .notes(dto.getNotes())
                .build();
}

    public FixedAssetDTO toDto(FixedAsset asset) {
        FixedAssetDTO dto = FixedAssetDTO.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .name(asset.getName())
                .categoryId(asset.getCategoryId())
                .managingUnitId(asset.getManagingUnitId())
                .serialNumber(asset.getSerialNumber())
                .manufacturer(asset.getManufacturer())
                .model(asset.getModel())
                .countryOfOrigin(asset.getCountryOfOrigin())
                .technicalSpecs(asset.getTechnicalSpecs())
                .location(asset.getLocation())
                .originalCost(asset.getOriginalCost())
                .acquisitionDate(asset.getAcquisitionDate())
                .usefulLifeYears(asset.getUsefulLifeYears())
                .salvageValue(asset.getSalvageValue())
                .depreciationMethod(asset.getDepreciationMethod())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .netBookValue(asset.getNetBookValue())
                .status(asset.getStatus())
                .notes(asset.getNotes())
                .build();

        lookupService.findAssetCategory(asset.getCategoryId()).ifPresent(c -> {
            dto.setCategoryCode(c.getCode());
            dto.setCategoryName(c.getName());
        });
        lookupService.findUnit(asset.getManagingUnitId()).ifPresent(u -> {
            dto.setManagingUnitCode(u.getCode());
            dto.setManagingUnitName(u.getName());
        });

        applyDepreciationExtras(dto, asset);
        return dto;
    }

    private void applyDepreciationExtras(FixedAssetDTO dto, FixedAsset asset) {
        if (asset.getUsefulLifeYears() == null || asset.getUsefulLifeYears() <= 0) {
            return;
        }
        dto.setDepreciationStartDate(asset.getAcquisitionDate());
        dto.setDepreciationEndDate(asset.getAcquisitionDate().plusYears(asset.getUsefulLifeYears()));

        BigDecimal depreciableBase = asset.getOriginalCost()
                .subtract(asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO);
        BigDecimal annualAmount = depreciableBase
                .divide(BigDecimal.valueOf(asset.getUsefulLifeYears()), 2, RoundingMode.HALF_UP);
        dto.setAnnualDepreciationAmount(annualAmount);
        dto.setAnnualDepreciationRate(
                BigDecimal.valueOf(100.0 / asset.getUsefulLifeYears())
                        .setScale(4, RoundingMode.HALF_UP));
    }
}
