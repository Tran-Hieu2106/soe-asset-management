package vn.edu.hust.soict.soe.assetmanagement.liquidation.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response shape for liquidation data.
 * Enriched with asset details for frontend display.
 */
@Getter
@Builder
public class LiquidationDto {
    private UUID id;
    private String requestCode;
    
    // Enriched Asset Data (M2)
    private UUID assetId;
    private String assetCode;
    private String assetName;
    
    private String initiatedBy;
    private LiquidationStatus status;
    private String justification;
    private BigDecimal estimatedValue;
    private String disposalMethod;
    private String notes;

    public static LiquidationDto from(LiquidationRequest request, String assetCode, String assetName) {
        return LiquidationDto.builder()
                .id(request.getId())
                .requestCode(request.getRequestCode())
                .assetId(request.getAssetId())
                .assetCode(assetCode)
                .assetName(assetName)
                .initiatedBy(request.getInitiatedBy())
                .status(request.getStatus())
                .justification(request.getJustification())
                .estimatedValue(request.getEstimatedValue())
                .disposalMethod(request.getDisposalMethod())
                .notes(request.getNotes())
                .build();
    }
}