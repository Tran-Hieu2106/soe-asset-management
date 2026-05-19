package vn.edu.hust.soict.soe.assetmanagement.handover.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response shape for handover data.
 * Enriched with asset details to support document generation (HL-03).
 */
@Getter
@Builder
public class HandoverDto {
    private UUID id;
    private String requestCode;
    
    // Asset references for the printable document
    private UUID assetId;
    private String assetCode;
    private String assetName;
    
    private UUID fromUnitId;
    private UUID toUnitId;
    private String initiatedBy;
    private HandoverStatus status;
    private String reason;
    private LocalDate handoverDate;
    private String assetCondition;
    private String notes;

    public static HandoverDto from(HandoverRequest request, String assetCode, String assetName) {
        return HandoverDto.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .assetId(request.getAssetId())
                .assetCode(assetCode)
                .assetName(assetName)
                .fromUnitId(request.getFromUnitId())
                .toUnitId(request.getToUnitId())
                .initiatedBy(request.getInitiatedBy())
                .status(request.getStatus())
                .reason(request.getReason())
                .handoverDate(request.getHandoverDate())
                .assetCondition(request.getAssetCondition())
                .notes(request.getNotes())
                .build();
    }
}