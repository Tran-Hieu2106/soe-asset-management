package vn.edu.hust.soict.soe.assetmanagement.liquidation.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.CreateLiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.LiquidationDto;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;

/**
 * ==============================================================================
 * SERVICE: LiquidationMapperService
 * PURPOSE: Handles data transformation between JPA Entities and DTOs for the
 * liquidation module. Keeps the DTOs completely decoupled from the database schema.
 * ==============================================================================
 */
@Service
public class LiquidationMapperService {

    /**
    * Converts a LiquidationRequest entity to a LiquidationDto.
    * @param entity The LiquidationRequest JPA entity to convert.
    * @return A LiquidationDto with data mapped from the entity.
    */
    public LiquidationDto toDto(LiquidationRequest entity) {
        if (entity == null) {
            return null;
        }

        return LiquidationDto.builder()
                // Identity
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .assetId(entity.getAssetId())
                .requestingUnitId(entity.getRequestingUnitId())
                .initiatedBy(entity.getInitiatedBy())
                
                // Status
                .status(entity.getStatus())
                
                // Justification
                .justification(entity.getJustification())
                .assetCondition(entity.getAssetCondition())
                .currentMarketValue(entity.getCurrentMarketValue())
                .disposalMethod(entity.getDisposalMethod())
                .disposalNotes(entity.getDisposalNotes())
                
                // Step 1: Manager
                .managerApprovedBy(entity.getManagerApprovedBy())
                .managerApprovedAt(entity.getManagerApprovedAt())
                .managerNotes(entity.getManagerNotes())
                
                // Step 2: Director
                .directorApprovedBy(entity.getDirectorApprovedBy())
                .directorApprovedAt(entity.getDirectorApprovedAt())
                .directorNotes(entity.getDirectorNotes())
                
                // Completion
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
                .finalDisposalValue(entity.getFinalDisposalValue())
                
                // Rejection
                .rejectedBy(entity.getRejectedBy())
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                
                // Document
                .documentRef(entity.getDocumentRef())
                .documentGeneratedAt(entity.getDocumentGeneratedAt())
                .documentSigned(entity.getDocumentSigned())
                
                // Audit
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    /**
     * Converts a CreateLiquidationRequest DTO to a LiquidationRequest entity.
     * @param dto The CreateLiquidationRequest DTO from the API request.
     * @param initiatedBy The username of the user initiating the request.
     * @param requestCode The generated request code for this liquidation.
     * @return A LiquidationRequest entity ready to be saved to the database.
     */
    public LiquidationRequest toEntity(CreateLiquidationRequest dto, String initiatedBy, String requestCode) {
        if (dto == null) {
            return null;
        }

        return LiquidationRequest.builder()
                .requestCode(requestCode)
                .assetId(dto.getAssetId())
                .requestingUnitId(dto.getRequestingUnitId())
                .initiatedBy(initiatedBy)
                .status(LiquidationStatus.DRAFT) // New requests always start as DRAFT
                .justification(dto.getJustification())
                .assetCondition(dto.getAssetCondition())
                .currentMarketValue(dto.getCurrentMarketValue())
                .disposalMethod(dto.getDisposalMethod())
                .disposalNotes(dto.getDisposalNotes())
                .documentSigned(false)
                .build();
    }
}