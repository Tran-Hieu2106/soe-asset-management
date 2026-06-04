package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;

/**
 * ==============================================================================
 * SERVICE: HandoverMapperService
 * PURPOSE: Handles data transformation between JPA Entities and DTOs for the
 * handover module. Keeps the DTOs completely decoupled from the database schema.
 * ==============================================================================
 */
@Service
public class HandoverMapperService {
        /**
        * Converts a CreateHandoverRequest DTO to a HandoverRequest entity.
        * @param dto The incoming DTO from the API layer.
        * @param initiatedBy The username of the user initiating the request.
        * @param requestCode The generated unique code for this handover request.
        * @return A HandoverRequest entity ready for persistence.
        */
    public HandoverRequest toEntity(CreateHandoverRequest dto, String initiatedBy, String requestCode) {
        if (dto == null) {
            return null;
        }

        return HandoverRequest.builder()
                .requestCode(requestCode)
                .assetId(dto.getAssetId())
                .fromUnitId(dto.getFromUnitId())
                .toUnitId(dto.getToUnitId())
                .initiatedBy(initiatedBy)
                .status(HandoverStatus.DRAFT) // All new requests start as DRAFT
                .reason(dto.getReason())
                .handoverDate(dto.getHandoverDate())
                .assetCondition(dto.getAssetCondition())
                .notes(dto.getNotes())
                .documentSigned(false)        // Document will be generated/signed later in the process
                .build();
    }

    /**
    * Converts a HandoverRequest entity to a HandoverDto.
    * @param entity The HandoverRequest JPA entity to convert.
    * @return A HandoverDto with data mapped from the entity.
    */
    public HandoverDto toDto(HandoverRequest entity) {
        if (entity == null) {
            return null;
        }

        return HandoverDto.builder()
                // Identity
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .assetId(entity.getAssetId())
                
                // Parties
                .fromUnitId(entity.getFromUnitId())
                .toUnitId(entity.getToUnitId())
                .initiatedBy(entity.getInitiatedBy())
                
                // Status
                .status(entity.getStatus())
                
                // Justification
                .reason(entity.getReason())
                .handoverDate(entity.getHandoverDate())
                .assetCondition(entity.getAssetCondition())
                .notes(entity.getNotes())
                
                // Step 1: Approval
                .deptApprovedBy(entity.getDeptApprovedBy())
                .deptApprovedAt(entity.getDeptApprovedAt())
                .deptApprovalNotes(entity.getDeptApprovalNotes())
                
                // Step 2: Confirmation
                .confirmedBy(entity.getConfirmedBy())
                .confirmedAt(entity.getConfirmedAt())
                .confirmationNotes(entity.getConfirmationNotes())
                
                // Step 3: Completion
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
                
                // Rejection
                .rejectedBy(entity.getRejectedBy())
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                
                // Document
                .documentRef(entity.getDocumentRef())
                .documentGeneratedAt(entity.getDocumentGeneratedAt())
                .documentSigned(entity.getDocumentSigned())
                
                // Audit (from BaseEntity)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}