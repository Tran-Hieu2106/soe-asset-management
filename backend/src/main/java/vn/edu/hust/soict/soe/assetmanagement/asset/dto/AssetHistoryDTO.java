package vn.edu.hust.soict.soe.assetmanagement.asset.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * DTO: AssetHistoryDTO
 * PURPOSE: Data shape returned when querying an asset's lifecycle history.
 * Ensures the AssetHistory JPA entity is never exposed directly via the API.
 * ==============================================================================
 */
@Getter
@Builder
public class AssetHistoryDTO {
    private UUID id;
    private UUID assetId;
    private String eventType;
    private String description;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;
}