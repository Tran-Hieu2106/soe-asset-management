package vn.edu.hust.soict.soe.assetmanagement.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CS-01: Material catalogue
 * Maps to: materials table (V3__create_stock.sql)
 */
@Entity
@Table(name = "materials", indexes = {
        @Index(name = "idx_materials_code",     columnList = "material_code"),
        @Index(name = "idx_materials_category", columnList = "category_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Material {

    @Id
    private UUID id;

    @Column(name = "material_code", nullable = false, unique = true, length = 50)
    private String materialCode;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MaterialCategory category;

    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure;

    @Column(name = "technical_specs", columnDefinition = "TEXT")
    private String technicalSpecs;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "supplier_code", length = 100)
    private String supplierCode;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "minimum_stock", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
    }
}
