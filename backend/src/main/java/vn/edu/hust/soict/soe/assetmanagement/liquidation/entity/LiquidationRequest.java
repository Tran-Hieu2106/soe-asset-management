package vn.edu.hust.soict.soe.assetmanagement.liquidation.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hust.soict.soe.assetmanagement.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Liquidation request entity (HL-02).
 * Maps to the `liquidation_requests` table.
 * Extends BaseEntity to automatically handle createdAt, updatedAt, and createdBy.
 */
@Entity
@Table(name = "liquidation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class LiquidationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_code", nullable = false, unique = true)
    private String requestCode;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private LiquidationStatus status = LiquidationStatus.PENDING;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;

    // e.g., AUCTION, SCRAP, DONATION
    @Column(name = "disposal_method", nullable = false, length = 50)
    private String disposalMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;
}