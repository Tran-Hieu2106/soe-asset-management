package vn.edu.hust.soict.soe.assetmanagement.liquidation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for initiating a new liquidation request (HL-02).
 */
@Getter
@Setter
public class CreateLiquidationRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotBlank(message = "Justification is required for liquidation")
    private String justification;

    @NotNull(message = "Estimated valuation is required")
    @DecimalMin(value = "0.0", message = "Estimated value must be at least 0")
    private BigDecimal estimatedValue;

    @NotBlank(message = "Disposal method (AUCTION, SCRAP, DONATION) is required")
    private String disposalMethod;

    private String notes;
}