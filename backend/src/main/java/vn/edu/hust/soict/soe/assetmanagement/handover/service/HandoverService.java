package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;

// M2 Integrations
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Handover service (HL-01, HL-03).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverService {

    private final HandoverRepository handoverRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetService fixedAssetService;

    @Transactional
    public HandoverDto createRequest(@NonNull CreateHandoverRequest dto, @NonNull String username) {
        if (handoverRepository.existsByAssetIdAndStatus(dto.getAssetId(), HandoverStatus.PENDING)) {
            throw new BusinessRuleException("A PENDING handover request already exists for this asset.");
        }

        FixedAsset asset = getAssetOrThrow(dto.getAssetId());
        String requestCode = "BG-" + System.currentTimeMillis();

        HandoverRequest request = HandoverRequest.builder()
                .requestCode(requestCode)
                .assetId(dto.getAssetId())
                .fromUnitId(dto.getFromUnitId())
                .toUnitId(dto.getToUnitId())
                .initiatedBy(username)
                .reason(dto.getReason())
                .handoverDate(dto.getHandoverDate())
                .assetCondition(dto.getAssetCondition())
                .notes(dto.getNotes())
                .status(HandoverStatus.PENDING)
                .build();

        HandoverRequest savedRequest = handoverRepository.save(Objects.requireNonNull(request));
        logAudit("CREATE", savedRequest, null);

        return HandoverDto.from(savedRequest, asset.getAssetCode(), asset.getName());
    }

    @Transactional
    public HandoverDto approveRequest(@NonNull UUID id, @NonNull String approverUsername) {
        HandoverRequest request = getRequestOrThrow(id);
        FixedAsset asset = getAssetOrThrow(request.getAssetId());

        if (request.getInitiatedBy().equals(approverUsername)) {
            throw new BusinessRuleException("Initiator cannot approve their own request.");
        }
        if (request.getStatus() != HandoverStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING requests can be approved.");
        }

        String oldJson = toJson(request);
        
        request.setStatus(HandoverStatus.APPROVED);
        HandoverRequest updatedRequest = handoverRepository.save(request);

        // M2 Integration execution
        fixedAssetService.updateAssetStatus(request.getAssetId(), AssetStatus.TRANSFERRED, "Handover approved", approverUsername);
        // Note: Hai needs to expose an updateManagingUnit(assetId, toUnitId) method in his FixedAssetService to complete the transfer.

        logAudit("APPROVE", updatedRequest, oldJson);

        return HandoverDto.from(updatedRequest, asset.getAssetCode(), asset.getName());
    }

    @Transactional
    public HandoverDto rejectRequest(@NonNull UUID id, @NonNull String approverUsername, String reason) {
        HandoverRequest request = getRequestOrThrow(id);
        FixedAsset asset = getAssetOrThrow(request.getAssetId());

        if (request.getInitiatedBy().equals(approverUsername)) {
            throw new BusinessRuleException("Initiator cannot reject their own request.");
        }
        if (request.getStatus() != HandoverStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING requests can be rejected.");
        }

        String oldJson = toJson(request);
        
        request.setStatus(HandoverStatus.REJECTED);
        if (reason != null && !reason.trim().isEmpty()) {
            request.setNotes(reason);
        }
        
        HandoverRequest updatedRequest = handoverRepository.save(request);
        logAudit("REJECT", updatedRequest, oldJson);

        return HandoverDto.from(updatedRequest, asset.getAssetCode(), asset.getName());
    }

    /**
     * Fulfills HL-03: Returns the enriched DTO for generating the printable document.
     */
    @Transactional(readOnly = true)
    public HandoverDto getDocumentData(@NonNull UUID id) {
        HandoverRequest request = getRequestOrThrow(id);
        FixedAsset asset = getAssetOrThrow(request.getAssetId());
        return HandoverDto.from(request, asset.getAssetCode(), asset.getName());
    }

    // --- Helper Methods ---

    private HandoverRequest getRequestOrThrow(UUID id) {
        return handoverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Handover request not found: " + id));
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

    private void logAudit(String action, HandoverRequest request, String oldJson) {
        auditLogService.log("HANDOVER", action, request.getId().toString(), request.getRequestCode(), oldJson, toJson(request), "Handover " + action);
    }
}