package vn.edu.hust.soict.soe.assetmanagement.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetSpecifications;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.lookup.service.LookupService;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetReportService {

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetService fixedAssetService;
    private final LookupService lookupService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AssetReportDto> generateAssetReport(
            AssetStatus status,
            Integer categoryId,
            UUID managingUnitId,
            LocalDate acquisitionFrom,
            LocalDate acquisitionTo,
            Pageable pageable) {

        Page<FixedAsset> page = fixedAssetRepository.findAll(
                AssetSpecifications.filter(status, categoryId, managingUnitId, acquisitionFrom, acquisitionTo, null),
                pageable);

        auditLogService.log("REPORT", "GENERATE_ASSET_REPORT", null, null,
                "{}", "{\"recordCount\": " + page.getTotalElements() + "}",
                "Generated asset inventory report");

        return page.map(this::toReportDto);
    }

    @Transactional(readOnly = true)
    public List<AssetReportDto> generateFullAssetRegister(
            AssetStatus status,
            Integer categoryId,
            UUID managingUnitId,
            LocalDate acquisitionFrom,
            LocalDate acquisitionTo) {

        return fixedAssetRepository.findAll(
                        AssetSpecifications.filter(status, categoryId, managingUnitId, acquisitionFrom, acquisitionTo, null))
                .stream()
                .map(asset -> {
                    FixedAsset computed = fixedAssetService.calculateCurrentDepreciation(asset.getId());
                    return toReportDto(computed);
                })
                .collect(Collectors.toList());
    }

    private AssetReportDto toReportDto(FixedAsset asset) {
        String categoryName = lookupService.findAssetCategory(asset.getCategoryId())
                .map(c -> c.getName()).orElse("N/A");
        String unitName = lookupService.findUnit(asset.getManagingUnitId())
                .map(u -> u.getName()).orElse("N/A");
        return AssetReportDto.from(asset, categoryName, unitName);
    }
}
