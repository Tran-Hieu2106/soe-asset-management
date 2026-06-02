package vn.edu.hust.soict.soe.assetmanagement.asset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * ENTITY: AssetHistory
 * PURPOSE: Maps to `asset_history` table. Fulfills requirement FA-04.
 * RULE CHECK: DOES NOT extend BaseEntity. Per database-schema.md, this is an 
 * append-only ledger and must NOT have an updated_at column.
 * ==============================================================================
 */
@Entity
@Table(name = "asset_history")
@Getter // Lombok annotation to generate getters for all fields
@Setter // Lombok annotation to generate setters for all fields
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    // Explicitly tracked creation time. No update time allowed.
    @Column(name = "performed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();
}