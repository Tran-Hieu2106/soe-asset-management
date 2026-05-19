package vn.edu.hust.soict.soe.assetmanagement.handover.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hust.soict.soe.assetmanagement.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Handover request entity (HL-01).
 * Maps to the `handover_requests` table.
 */
@Entity
@Table(name = "handover_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_code", nullable = false, unique = true)
    private String requestCode;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "from_unit_id", nullable = false)
    private UUID fromUnitId;

    @Column(name = "to_unit_id", nullable = false)
    private UUID toUnitId;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private HandoverStatus status = HandoverStatus.PENDING;

    private String reason;
    
    @Column(name = "handover_date")
    private LocalDate handoverDate;

    @Column(name = "asset_condition")
    private String assetCondition;

    private String notes;
}