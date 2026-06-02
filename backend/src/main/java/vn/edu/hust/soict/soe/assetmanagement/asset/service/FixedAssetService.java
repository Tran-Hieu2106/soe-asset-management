package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * ==============================================================================
 * SERVICE: FixedAssetService
 * PURPOSE: Core business logic for M2 (FA-01 -> FA-04).
 * RULE CHECK: 
 * - Throws global Exceptions (ResourceNotFoundException, BusinessRuleException).
 * - Integrates `AuditLogService` for RP-03.
 * - Provides Handover hooks (`updateAssetStatusAndUnit`).
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final AuditLogService auditLogService; // M4 Global Audit Integration

    // ── READ (FA-01) ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<FixedAsset> getAllAssets(Pageable pageable) {
        return fixedAssetRepository.findAll(pageable);
    }

    // ── CREATE (FA-01) ────────────────────────────────────────────────────
    public FixedAsset createAsset(FixedAssetDTO dto, String username) {
        if (fixedAssetRepository.existsByAssetCode(dto.getAssetCode())) {
            throw new BusinessRuleException("Mã tài sản đã tồn tại trong hệ thống.");
        }

        FixedAsset asset = FixedAsset.builder()
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
                .depreciationMethod(dto.getDepreciationMethod() != null ? dto.getDepreciationMethod() : "STRAIGHT_LINE")
                .notes(dto.getNotes())
                .build();

        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        // FA-04: Asset-Specific History
        saveHistoryLog(savedAsset.getId(), "CREATED", "Khởi tạo hồ sơ tài sản", "{}", "{}", username);

        // RP-03: Global System Audit
        auditLogService.log("ASSET", "CREATE", savedAsset.getId().toString(), savedAsset.getAssetCode(), 
                "{}", "{\"name\": \"" + savedAsset.getName() + "\"}", "Đăng ký tài sản mới");

        return savedAsset;
    }

    // ── DEPRECIATION ENGINE (FA-02) ───────────────────────────────────────
    /**
     * Calculates depreciation on-the-fly based on Circular 45/2013/TT-BTC.
     */
    public FixedAsset calculateCurrentDepreciation(UUID id) {
        FixedAsset asset = getAssetOrThrow(id);

        if ("DECLINING_BALANCE".equalsIgnoreCase(asset.getDepreciationMethod())) {
            return calculateDecliningBalance(asset);
        } else {
            return calculateStraightLine(asset);
        }
    }

    private FixedAsset calculateStraightLine(FixedAsset asset) {
        if (asset.getUsefulLifeYears() <= 0) return asset;

        long totalMonths = asset.getUsefulLifeYears() * 12L;
        long monthsUsed = Math.max(0, ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now()));

        BigDecimal depreciableBase = asset.getOriginalCost().subtract(asset.getSalvageValue());
        
        BigDecimal accumulated;
        if (monthsUsed >= totalMonths) {
            accumulated = depreciableBase;
        } else {
            accumulated = depreciableBase.multiply(BigDecimal.valueOf(monthsUsed))
                            .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        }

        asset.setAccumulatedDepreciation(accumulated);
        asset.setNetBookValue(asset.getOriginalCost().subtract(accumulated));
        return asset; 
    }

    private FixedAsset calculateDecliningBalance(FixedAsset asset) {
        // Simplified mathematical hook for Declining Balance per Circular 45.
        // Falls back to straight line logic in this stub to ensure compilation.
        return calculateStraightLine(asset);
    }

    // ── STATUS UPDATES & M4 INTEGRATION HOOKS (FA-03) ─────────────────────
    
    /**
     * Standard status update (e.g., set to MAINTENANCE, or LIQUIDATED by M4).
     */
    public FixedAsset updateAssetStatus(UUID id, AssetStatus newStatus, String reason, String username) {
        return updateAssetInternal(id, newStatus, null, reason, username);
    }

    /**
     * Advanced status update (e.g., set to TRANSFERRED and update managingUnitId by M4 Handover).
     */
    public FixedAsset updateAssetStatusAndUnit(UUID id, AssetStatus newStatus, UUID newUnitId, String reason, String username) {
        return updateAssetInternal(id, newStatus, newUnitId, reason, username);
    }

    private FixedAsset updateAssetInternal(UUID id, AssetStatus newStatus, UUID newUnitId, String reason, String username) {
        FixedAsset asset = getAssetOrThrow(id);
        
        String oldStatus = asset.getStatus().toString();
        String oldUnit = asset.getManagingUnitId().toString();

        asset.setStatus(newStatus);
        if (newUnitId != null) {
            asset.setManagingUnitId(newUnitId);
        }

        FixedAsset updatedAsset = fixedAssetRepository.save(asset);

        // FA-04: Append to local asset ledger
        saveHistoryLog(updatedAsset.getId(), "STATUS_UPDATE", reason, oldStatus, newStatus.toString(), username);

        // RP-03: Append to global audit log
        auditLogService.log("ASSET", "UPDATE", updatedAsset.getId().toString(), updatedAsset.getAssetCode(), 
                "{\"status\": \"" + oldStatus + "\", \"unit\": \"" + oldUnit + "\"}", 
                "{\"status\": \"" + newStatus + "\", \"unit\": \"" + newUnitId + "\"}", 
                reason);

        return updatedAsset;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private FixedAsset getAssetOrThrow(UUID id) {
        return fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài sản với ID: " + id));
    }

    private void saveHistoryLog(UUID assetId, String eventType, String description, String oldValue, String newValue, String performedBy) {
        AssetHistory history = AssetHistory.builder()
                .assetId(assetId)
                .eventType(eventType)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .build();
        assetHistoryRepository.save(history);
    }
}