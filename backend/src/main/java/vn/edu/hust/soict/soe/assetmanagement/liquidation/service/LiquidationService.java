package vn.edu.hust.soict.soe.assetmanagement.liquidation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.CreateLiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.LiquidationDto;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.repository.LiquidationRepository;

// M2 Integrations
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;

import java.util.UUID;

/**
 * Liquidation service (HL-02).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidationService {

    private final LiquidationRepository liquidationRepository;
    private final HandoverRepository handoverRepository; // To check for cross-module conflicts
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetService fixedAssetService;

    @Transactional
    public LiquidationDto createRequest(@NonNull CreateLiquidationRequest dto, @NonNull String username) {
        
        // Rule 1: Check for existing pending liquidations
        if (liquidationRepository.existsByAssetIdAndStatus(dto.getAssetId(), LiquidationStatus.PENDING)) {
            throw new BusinessRuleException("A PENDING liquidation request already exists for this asset.");
        }
        
        // Rule 2: Cross-module check. Ensure asset isn't currently being handed over.
        if (handoverRepository.existsByAssetIdAndStatus(dto.getAssetId(), HandoverStatus.PENDING)) {
            throw new BusinessRuleException("Cannot liquidate asset. A PENDING handover request exists for this asset.");
        }

        FixedAsset asset = getAssetOrThrow(dto.getAssetId());
        String requestCode = "TL-" + System.currentTimeMillis();

        LiquidationRequest request = LiquidationRequest.builder()
                .requestCode(requestCode)
                .assetId(dto.getAssetId())
                .initiatedBy(username)
                .justification(dto.getJustification())
                .estimatedValue(dto.getEstimatedValue())
                .disposalMethod(dto.getDisposalMethod().toUpperCase())
                .notes(dto.getNotes())
                .status(LiquidationStatus.PENDING)
                .build();

        LiquidationRequest savedRequest = liquidationRepository.save(request);
        logAudit("CREATE", savedRequest, "{}");

        return LiquidationDto.from(savedRequest, asset.getAssetCode(), asset.getName());
    }

    @Transactional
    public LiquidationDto approveRequest(@NonNull UUID id, @NonNull String approverUsername) {
        LiquidationRequest request = getRequestOrThrow(id);
        FixedAsset asset = getAssetOrThrow(request.getAssetId());

        if (request.getInitiatedBy().equals(approverUsername)) {
            throw new BusinessRuleException("Initiator cannot approve their own liquidation request.");
        }
        if (request.getStatus() != LiquidationStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING requests can be approved.");
        }

        String oldJson = toJson(request);
        request.setStatus(LiquidationStatus.APPROVED);
        LiquidationRequest updatedRequest = liquidationRepository.save(request);

        // M2 Integration: Set asset status to LIQUIDATED
        fixedAssetService.updateAssetStatus(
            request.getAssetId(), 
            AssetStatus.LIQUIDATED, 
            "Liquidation approved via " + request.getDisposalMethod(), 
            approverUsername
        );

        logAudit("APPROVE", updatedRequest, oldJson);

        return LiquidationDto.from(updatedRequest, asset.getAssetCode(), asset.getName());
    }

    @Transactional
    public LiquidationDto rejectRequest(@NonNull UUID id, @NonNull String approverUsername, String reason) {
        LiquidationRequest request = getRequestOrThrow(id);
        FixedAsset asset = getAssetOrThrow(request.getAssetId());

        if (request.getInitiatedBy().equals(approverUsername)) {
            throw new BusinessRuleException("Initiator cannot reject their own liquidation request.");
        }
        if (request.getStatus() != LiquidationStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING requests can be rejected.");
        }

        String oldJson = toJson(request);
        request.setStatus(LiquidationStatus.REJECTED);
        
        if (reason != null && !reason.trim().isEmpty()) {
            request.setNotes(reason); // Store rejection reason
        }
        
        LiquidationRequest updatedRequest = liquidationRepository.save(request);
        logAudit("REJECT", updatedRequest, oldJson);

        return LiquidationDto.from(updatedRequest, asset.getAssetCode(), asset.getName());
    }

    // --- Helper Methods ---

    private LiquidationRequest getRequestOrThrow(UUID id) {
        return liquidationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidation request not found: " + id));
    }

    private FixedAsset getAssetOrThrow(UUID assetId) {
        return fixedAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON parse error", e);
            return "{}";
        }
    }

    private void logAudit(String action, LiquidationRequest request, String oldJson) {
        String safeOldJson = (oldJson != null) ? oldJson : "{}";
        auditLogService.log(
            "LIQUIDATION", 
            action, 
            request.getId().toString(), 
            request.getRequestCode(), 
            safeOldJson, 
            toJson(request), 
            "Liquidation " + action
        );
    }
}