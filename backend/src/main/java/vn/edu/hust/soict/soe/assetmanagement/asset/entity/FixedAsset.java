package vn.edu.hust.soict.soe.assetmanagement.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ==============================================================================
 * ENTITY: FixedAsset
 * PURPOSE: Maps exactly to the `assets` table in V2__create_assets.sql.
 * RULE CHECK: Extends BaseEntity to automatically inherit createdAt, updatedAt, 
 * and createdBy (via JPA Auditing configured in AuditConfig).
 * ==============================================================================
 */
@Entity
@Table(name = "assets", indexes = {
    @Index(name = "idx_assets_code", columnList = "asset_code")
})
@Getter // Lombok annotation to generate getters for all fields
@Setter // Lombok annotation to generate setters for all fields
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true) // Crucial when extending BaseEntity
public class FixedAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_code", nullable = false, unique = true, length = 50)
    private String assetCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // Handover module relies on this field to transfer ownership
    @Column(name = "managing_unit_id", nullable = false)
    private UUID managingUnitId;

    // --- Technical Specs ---
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    private String manufacturer;
    private String model;
    
    @Column(name = "country_of_origin", length = 100)
    private String countryOfOrigin;
    
    @Column(name = "technical_specs", columnDefinition = "TEXT")
    private String technicalSpecs;
    private String location;

    // --- Financials (Rule: NUMERIC(18,2) per database-schema.md) ---
    @Column(name = "original_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalCost;

    @Column(name = "salvage_value", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "net_book_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal netBookValue;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "useful_life_years", nullable = false)
    private Integer usefulLifeYears;

    @Column(name = "depreciation_method", nullable = false, length = 20)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE";

    // --- Status ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;
}