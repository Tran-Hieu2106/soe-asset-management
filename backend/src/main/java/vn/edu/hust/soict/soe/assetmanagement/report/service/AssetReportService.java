package vn.edu.hust.soict.soe.assetmanagement.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating Asset Reports (RP-01).
 * Deeply integrates with FixedAssetRepository (M2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetReportService {

    private final FixedAssetRepository fixedAssetRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AssetReportDto> generateFullAssetRegister() {
        log.info("Generating Full Asset Register Report...");

        // Fetch all assets
        List<FixedAsset> allAssets = fixedAssetRepository.findAll();

        // Log the reporting action in the global audit trail
        auditLogService.log("REPORT", "GENERATE_ASSET_REPORT", null, null, 
                "{}", "{\"recordCount\": " + allAssets.size() + "}", "Generated full asset register report");

        return allAssets.stream()
                .map(AssetReportDto::from)
                .collect(Collectors.toList());
    }
}