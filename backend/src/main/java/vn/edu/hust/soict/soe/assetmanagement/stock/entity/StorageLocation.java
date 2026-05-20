package vn.edu.hust.soict.soe.assetmanagement.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hust.soict.soe.assetmanagement.common.BaseEntity;

import java.util.UUID;

/**
 * Maps to: storage_locations table (V3__create_stock.sql)
 */
@Entity
@Table(name = "storage_locations")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@EqualsAndHashCode(callSuper = true)
public class StorageLocation extends BaseEntity { // Extends BaseEntity for common fields

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    // FK to managing_units (owned by user module — ID reference only)
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
    }
}