package vn.edu.hust.soict.soe.assetmanagement.handover.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload for creating a new handover request (HL-01).
 */
@Getter
@Setter
public class CreateHandoverRequest {
    
    @NotNull(message = "Asset ID is required")
    private UUID assetId;
    
    @NotNull(message = "Origin unit ID is required")
    private UUID fromUnitId;
    
    @NotNull(message = "Destination unit ID is required")
    private UUID toUnitId;
    
    private String reason;
    private LocalDate handoverDate;
    private String assetCondition;
    private String notes;
}