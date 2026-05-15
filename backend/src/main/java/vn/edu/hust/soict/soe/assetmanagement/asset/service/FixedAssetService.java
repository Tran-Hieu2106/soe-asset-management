package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class FixedAssetService {

    @Autowired
    private FixedAssetRepository fixedAssetRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

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
        int usefulLifeYears = asset.getUsefulLifeYears();
        if (usefulLifeYears <= 0) return asset;

        // Annual Depr = Original Cost / Useful Life
        BigDecimal yearlyDepr = originalCost.divide(BigDecimal.valueOf(usefulLifeYears), 2, RoundingMode.HALF_UP);
        // Monthly Depr = Annual Depr / 12
        BigDecimal monthlyDepr = yearlyDepr.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        long monthsUsed = ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now());
        
        BigDecimal accumulated = monthlyDepr.multiply(BigDecimal.valueOf(Math.max(0, monthsUsed)));
        updateFinancials(asset, accumulated);
        return asset;
    }

    /**
     * LOGIC 2: Adjusted Declining Balance Depreciation (Circular 45/2013/TT-BTC).
     */
    private FixedAsset calculateDecliningBalance(FixedAsset asset) {
        BigDecimal originalCost = asset.getOriginalCost();
        int T = asset.getUsefulLifeYears();
        long monthsUsed = ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now());

        if (T <= 0 || monthsUsed <= 0) return asset;

        // 1. Determine the adjustment coefficient based on useful life
        double multiplier = (T <= 4) ? 1.5 : (T <= 6) ? 2.0 : 2.5;
        
        // 2. Accelerated Depreciation Rate = (1 / T) * Multiplier
        double annualRate = (1.0 / T) * multiplier;
        
        BigDecimal remainingValue = originalCost;
        int fullYearsUsed = (int) (monthsUsed / 12);
        int remainingMonths = (int) (monthsUsed % 12);

        // 3. Iterative calculation of accumulated depreciation over years
        for (int i = 0; i < fullYearsUsed; i++) {
            BigDecimal yearlyDepr = remainingValue.multiply(BigDecimal.valueOf(annualRate));
            remainingValue = remainingValue.subtract(yearlyDepr);
        }

        // 4. Calculate remaining months for the current year
        BigDecimal currentYearDeprRate = remainingValue.multiply(BigDecimal.valueOf(annualRate));
        BigDecimal monthlyDepr = currentYearDeprRate.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        remainingValue = remainingValue.subtract(monthlyDepr.multiply(BigDecimal.valueOf(remainingMonths)));

        BigDecimal accumulated = originalCost.subtract(remainingValue);
        updateFinancials(asset, accumulated);
        return asset;
    }

    /**
     * Updates financial fields for the asset entity.
     */
    private void updateFinancials(FixedAsset asset, BigDecimal accumulated) {
        asset.setAccumulatedDepreciation(accumulated);
        BigDecimal netValue = asset.getOriginalCost().subtract(accumulated);
        asset.setNetBookValue(netValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netValue);
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