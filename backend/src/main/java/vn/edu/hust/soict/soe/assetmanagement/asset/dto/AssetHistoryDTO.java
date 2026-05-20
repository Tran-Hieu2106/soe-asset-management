package vn.edu.hust.soict.soe.assetmanagement.asset.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Asset History.
 * Ensures the AssetHistory JPA entity is never exposed directly via the API.
 */
public class AssetHistoryDTO {

    private UUID id;
    private UUID assetId;
    private String eventType;
    private String description;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;

    // --- GETTERS AND SETTERS ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}