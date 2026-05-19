package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNullFields;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.common.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Fixed asset service.
 * Handles asset registration, depreciation calculation, and status updates.
 */

@Service
public class FixedAssetService {

    @Autowired
    private FixedAssetRepository fixedAssetRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Retrieves all fixed assets from the database.
     */
    public List<FixedAsset> getAllAssets() {
        return fixedAssetRepository.findAll();
    }

    /**
     * FA-01 & FA-04: Creates a new digital asset profile and logs the event.
     */
    @Transactional
    public FixedAsset createAsset(FixedAssetDTO dto) {
        FixedAsset asset = new FixedAsset();
        
        // Mapping data from DTO to Entity
        asset.setAssetCode(dto.getAssetCode());
        asset.setName(dto.getName());
        asset.setCategoryId(dto.getCategoryId());
        asset.setManagingUnitId(dto.getManagingUnitId());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setManufacturer(dto.getManufacturer());
        asset.setModel(dto.getModel());
        asset.setCountryOfOrigin(dto.getCountryOfOrigin());
        asset.setTechnicalSpecs(dto.getTechnicalSpecs());
        asset.setLocation(dto.getLocation());
        asset.setOriginalCost(dto.getOriginalCost());
        asset.setAcquisitionDate(dto.getAcquisitionDate());
        asset.setUsefulLifeYears(dto.getUsefulLifeYears());
        
        // Default financial values
        asset.setSalvageValue(dto.getSalvageValue() != null ? dto.getSalvageValue() : BigDecimal.ZERO);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(dto.getOriginalCost());
        asset.setStatus(AssetStatus.IN_USE);
        
        // Default method set to STRAIGHT_LINE, can be modified via DTO if needed
        asset.setDepreciationMethod(dto.getDepreciationMethod() != null ? dto.getDepreciationMethod() : "STRAIGHT_LINE");
        
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        asset.setCreatedBy("system_test");

        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        // Save history log for asset creation
        saveHistoryLog(
            savedAsset.getId(), 
            "CREATED", 
            "Digital asset profile initialization", 
            null, 
            "Asset created with code: " + savedAsset.getAssetCode(),
            savedAsset.getCreatedBy()
        );

        auditLogService.log(
            "ASSET",                                         // String module
            "CREATE",                                        // String action
            savedAsset.getId().toString(),                          // String recordId
            savedAsset.getAssetCode(),                              // String recordCode
            "{}",                                         // String oldValue 
            "{\"name\": \"" + savedAsset.getName() + "\"}",         // String newValu
            "Registered new fixed asset"                // String description
        );

        return savedAsset;
    }

    /**
     * FA-02: Intelligent depreciation calculation engine.
     */
    public FixedAsset calculateCurrentDepreciation(UUID id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + id));

        String method = asset.getDepreciationMethod();
        
        // Determine which method to use for calculation
        if ("DECLINING_BALANCE".equalsIgnoreCase(method)) {
            return calculateDecliningBalance(asset);
        } else {
            return calculateStraightLine(asset);
        }
    }

    /**
     * LOGIC 1: Straight-Line Depreciation.
     */
    private FixedAsset calculateStraightLine(FixedAsset asset) {
        BigDecimal originalCost = asset.getOriginalCost();
        BigDecimal salvageValue = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
        int usefulLifeYears = asset.getUsefulLifeYears();

        if (usefulLifeYears <= 0) return asset;

        long totalMonths = usefulLifeYears * 12L;
        long monthsUsed = Math.max(0, ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now()));

        BigDecimal depreciableBase = originalCost.subtract(salvageValue);
        BigDecimal accumulated;

        if (monthsUsed >= totalMonths) {
            // TRUE-UP: Đã hết vòng đời, ép hao mòn bằng đúng Giá trị phải khấu hao
            accumulated = depreciableBase;
        } else {
            // CÁC THÁNG GIỮA KỲ: Giữ scale = 2 để đảm bảo độ chính xác hệ thống lõi
            accumulated = depreciableBase
                    .multiply(BigDecimal.valueOf(monthsUsed))
                    .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        }

        updateFinancials(asset, accumulated, salvageValue);
        return asset;
    }

    /**
     * LOGIC 2: Adjusted Declining Balance Depreciation (Circular 45/2013/TT-BTC).
     */
    private FixedAsset calculateDecliningBalance(FixedAsset asset) {
        BigDecimal originalCost = asset.getOriginalCost();
        BigDecimal salvageValue = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
        int totalYears = asset.getUsefulLifeYears();

        if (totalYears <= 0) return asset;

        long totalMonths = totalYears * 12L;
        long monthsUsed = Math.max(0, ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now()));

        BigDecimal depreciableBase = originalCost.subtract(salvageValue);

        // TRUE-UP: Đã hết vòng đời thì không cần chạy vòng lặp, gán thẳng số tối đa
        if (monthsUsed >= totalMonths) {
            updateFinancials(asset, depreciableBase, salvageValue);
            return asset;
        }

        // 1. Xác định hệ số điều chỉnh
        double multiplier = (totalYears <= 4) ? 1.5 : (totalYears <= 6) ? 2.0 : 2.5;
        double acceleratedRate = (1.0 / totalYears) * multiplier;

        BigDecimal remainingBase = depreciableBase;
        BigDecimal accumulated = BigDecimal.ZERO;

        int fullYearsUsed = (int) (monthsUsed / 12);
        int remainingMonths = (int) (monthsUsed % 12);
        boolean switchedToStraightLine = false;

        // 2. Chạy tính toán cho các NĂM chẵn đã qua
        for (int year = 1; year <= fullYearsUsed; year++) {
            int remainingYearsAtStart = totalYears - year + 1;
            
            // Tính các mức khấu hao với độ chính xác hệ thống (scale = 2)
            BigDecimal currentStraightLine = remainingBase.divide(BigDecimal.valueOf(remainingYearsAtStart), 2, RoundingMode.HALF_UP);
            BigDecimal currentAccelerated = remainingBase.multiply(BigDecimal.valueOf(acceleratedRate)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal yearlyDepr;
            // KIỂM TRA GIAO CẮT: Mức Giảm dần <= mức Bình quân -> Bẻ lái sang Đường thẳng
            if (switchedToStraightLine || currentAccelerated.compareTo(currentStraightLine) <= 0) {
                switchedToStraightLine = true;
                yearlyDepr = currentStraightLine;
            } else {
                yearlyDepr = currentAccelerated;
            }

            accumulated = accumulated.add(yearlyDepr);
            remainingBase = remainingBase.subtract(yearlyDepr);
        }

        // 3. Chạy tính toán cho các THÁNG LẺ của năm hiện tại
        if (remainingMonths > 0) {
            int currentYear = fullYearsUsed + 1;
            int remainingYearsAtStart = totalYears - currentYear + 1;

            BigDecimal currentStraightLine = remainingBase.divide(BigDecimal.valueOf(remainingYearsAtStart), 2, RoundingMode.HALF_UP);
            BigDecimal currentAccelerated = remainingBase.multiply(BigDecimal.valueOf(acceleratedRate)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal yearlyDeprForCurrentYear;
            if (switchedToStraightLine || currentAccelerated.compareTo(currentStraightLine) <= 0) {
                yearlyDeprForCurrentYear = currentStraightLine;
            } else {
                yearlyDeprForCurrentYear = currentAccelerated;
            }

            BigDecimal monthlyDepr = yearlyDeprForCurrentYear.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            accumulated = accumulated.add(monthlyDepr.multiply(BigDecimal.valueOf(remainingMonths)));
        }

        updateFinancials(asset, accumulated, salvageValue);
        return asset;
    }

    /**
     * Updates financial fields for the asset entity.
     */
    private void updateFinancials(FixedAsset asset, BigDecimal accumulated, BigDecimal salvageValue) {
        // Ép chuẩn format 2 chữ số thập phân trước khi lưu vào Database
        asset.setAccumulatedDepreciation(accumulated.setScale(2, RoundingMode.HALF_UP));
        
        BigDecimal netValue = asset.getOriginalCost().subtract(asset.getAccumulatedDepreciation());
        asset.setNetBookValue(netValue.max(salvageValue).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * FA-03 & FA-04: Updates operational status and logs the history event.
     */
    @Transactional
    public FixedAsset updateAssetStatus(UUID id, AssetStatus newStatus, String reason, String performedBy) {
        FixedAsset asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + id));

        String oldStatus = asset.getStatus().toString();
        asset.setStatus(newStatus);
        asset.setStatusReason(reason);
        asset.setStatusChangedAt(LocalDateTime.now());
        asset.setStatusChangedBy(performedBy);
        asset.setUpdatedAt(LocalDateTime.now());

        FixedAsset updatedAsset = fixedAssetRepository.save(asset);

        // Log status change event
        saveHistoryLog(
            updatedAsset.getId(),
            "STATUS_CHANGED",
            "Status updated to: " + newStatus,
            "Old: " + oldStatus,
            "New: " + newStatus + " | Reason: " + reason,
            performedBy
        );

        auditLogService.log(
            "ASSET",                         // String module
            "UPDATE_STATUS",                 // String action
            updatedAsset.getId().toString(),         // String recordId
            updatedAsset.getAssetCode(),             // String recordCode
            "{\"status\": \"" + oldStatus + "\"}",   // String oldValue
            "{\"status\": \"" + newStatus + "\"}",   // String newValue
            "Changed asset status to " + newStatus   // String description
        );
        
        return updatedAsset;
    }

    /**
     * Helper method to save history logs (FA-04).
     */
    private void saveHistoryLog(UUID assetId, String eventType, String description, String oldValue, String newValue, String performedBy) {
        AssetHistory history = new AssetHistory();
        history.setAssetId(assetId);
        history.setEventType(eventType);
        history.setDescription(description);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setPerformedBy(performedBy);
        history.setPerformedAt(LocalDateTime.now());
        
        assetHistoryRepository.save(history);
    }
}