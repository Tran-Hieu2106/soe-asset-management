package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException; // IMPORT GLOBAL EXCEPTION

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

    @Autowired
    private AuditLogService auditLogService;

    public List<FixedAsset> getAllAssets() {
        return fixedAssetRepository.findAll();
    }

    @Transactional
    public FixedAsset createAsset(FixedAssetDTO dto) {
        FixedAsset asset = new FixedAsset();
        
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
        
        asset.setSalvageValue(dto.getSalvageValue() != null ? dto.getSalvageValue() : BigDecimal.ZERO);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(dto.getOriginalCost());
        asset.setStatus(AssetStatus.IN_USE);
        asset.setDepreciationMethod(dto.getDepreciationMethod() != null ? dto.getDepreciationMethod() : "STRAIGHT_LINE");
        
        // Note: createdAt and updatedAt are now handled automatically by @EntityListeners(AuditingEntityListener.class)
        // inside Cuong's BaseEntity, but we leave createdBy here for the mock user testing.
        asset.setCreatedBy("system_test");

        FixedAsset savedAsset = fixedAssetRepository.save(asset);

        saveHistoryLog(
            savedAsset.getId(), 
            "CREATED", 
            "Digital asset profile initialization", 
            null, 
            "Asset created with code: " + savedAsset.getAssetCode(),
            savedAsset.getCreatedBy()
        );

        auditLogService.log(
            "ASSET", "CREATE", savedAsset.getId().toString(), savedAsset.getAssetCode(),
            "{}", "{\"name\": \"" + savedAsset.getName() + "\"}",
            "Registered new fixed asset"
        );

        return savedAsset;
    }

    public FixedAsset calculateCurrentDepreciation(UUID id) {
        // REPLACED RuntimeException WITH ResourceNotFoundException
        FixedAsset asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with ID: " + id));

        String method = asset.getDepreciationMethod();
        if ("DECLINING_BALANCE".equalsIgnoreCase(method)) {
            return calculateDecliningBalance(asset);
        } else {
            return calculateStraightLine(asset);
        }
    }

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
            accumulated = depreciableBase;
        } else {
            accumulated = depreciableBase
                    .multiply(BigDecimal.valueOf(monthsUsed))
                    .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        }

        updateFinancials(asset, accumulated, salvageValue);
        return asset;
    }

    private FixedAsset calculateDecliningBalance(FixedAsset asset) {
        BigDecimal originalCost = asset.getOriginalCost();
        BigDecimal salvageValue = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
        int totalYears = asset.getUsefulLifeYears();

        if (totalYears <= 0) return asset;

        long totalMonths = totalYears * 12L;
        long monthsUsed = Math.max(0, ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), LocalDate.now()));

        BigDecimal depreciableBase = originalCost.subtract(salvageValue);

        if (monthsUsed >= totalMonths) {
            updateFinancials(asset, depreciableBase, salvageValue);
            return asset;
        }

        double multiplier = (totalYears <= 4) ? 1.5 : (totalYears <= 6) ? 2.0 : 2.5;
        double acceleratedRate = (1.0 / totalYears) * multiplier;

        BigDecimal remainingBase = depreciableBase;
        BigDecimal accumulated = BigDecimal.ZERO;

        int fullYearsUsed = (int) (monthsUsed / 12);
        int remainingMonths = (int) (monthsUsed % 12);
        boolean switchedToStraightLine = false;

        for (int year = 1; year <= fullYearsUsed; year++) {
            int remainingYearsAtStart = totalYears - year + 1;
            
            BigDecimal currentStraightLine = remainingBase.divide(BigDecimal.valueOf(remainingYearsAtStart), 2, RoundingMode.HALF_UP);
            BigDecimal currentAccelerated = remainingBase.multiply(BigDecimal.valueOf(acceleratedRate)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal yearlyDepr;
            if (switchedToStraightLine || currentAccelerated.compareTo(currentStraightLine) <= 0) {
                switchedToStraightLine = true;
                yearlyDepr = currentStraightLine;
            } else {
                yearlyDepr = currentAccelerated;
            }

            accumulated = accumulated.add(yearlyDepr);
            remainingBase = remainingBase.subtract(yearlyDepr);
        }

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

    // Helper method to update accumulated depreciation and net book value
    private void updateFinancials(FixedAsset asset, BigDecimal accumulated, BigDecimal salvageValue) {
        asset.setAccumulatedDepreciation(accumulated.setScale(2, RoundingMode.HALF_UP));
        BigDecimal netValue = asset.getOriginalCost().subtract(asset.getAccumulatedDepreciation());
        asset.setNetBookValue(netValue.max(salvageValue).setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional
    public FixedAsset updateAssetStatus(UUID id, AssetStatus newStatus, String reason, String performedBy) {
        // REPLACED RuntimeException WITH ResourceNotFoundException
        FixedAsset asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with ID: " + id));

        String oldStatus = asset.getStatus().toString();
        asset.setStatus(newStatus);
        asset.setStatusReason(reason);
        asset.setStatusChangedAt(LocalDateTime.now());
        asset.setStatusChangedBy(performedBy);
        // Note: updatedAt is now handled automatically by BaseEntity

        FixedAsset updatedAsset = fixedAssetRepository.save(asset);

        saveHistoryLog(
            updatedAsset.getId(), "STATUS_CHANGED", "Status updated to: " + newStatus,
            "Old: " + oldStatus, "New: " + newStatus + " | Reason: " + reason, performedBy
        );

        auditLogService.log(
            "ASSET", "UPDATE_STATUS", updatedAsset.getId().toString(), updatedAsset.getAssetCode(),
            "{\"status\": \"" + oldStatus + "\"}", "{\"status\": \"" + newStatus + "\"}",
            "Changed asset status to " + newStatus
        );
        
        return updatedAsset;
    }

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