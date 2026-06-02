package vn.edu.hust.soict.soe.assetmanagement.liquidation.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * DTO: LiquidationDto
 * ==============================================================================
 * PURPOSE:
 *   The read-side (response) representation of a LiquidationRequest. Returned
 *   by ALL liquidation endpoints: GET list, GET by ID, POST create, and all
 *   PUT workflow actions (submit, approve manager, approve director, complete, reject).
 *
 * WHY A SEPARATE DTO:
 *   Per the project architecture rule: "Never expose JPA entity objects directly
 *   in API responses — use DTOs." This decouples the API contract from the
 *   internal data model. If a column is renamed in the DB, only the entity and
 *   this DTO's from() method need to change — not every controller or test.
 *
 * FULL FIELD COVERAGE:
 *   Every column from `liquidation_requests` is exposed in this DTO, including
 *   both approval steps' timestamps and notes, and the final disposal value.
 *   This lets the frontend render the complete audit trail of who did what
 *   at each step from a single API response.
 *
 * STATIC FACTORY PATTERN:
 *   The static method LiquidationDto.from(LiquidationRequest entity) is the
 *   ONLY conversion point. Always use this method — never build the DTO manually
 *   in service or controller code. Consistent with AuditLogDto.from() and
 *   HandoverDto.from() patterns in the audit and handover modules.
 *
 * USAGE EXAMPLE:
 *   LiquidationDto dto = LiquidationDto.from(savedRequest);
 *   return ResponseEntity.ok(ApiResponse.success("...", dto));
 * ==============================================================================
 */
@Getter
@Builder
public class LiquidationDto {

    // ── IDENTITY ──────────────────────────────────────────────────────────

    /** Internal UUID — used by the frontend for all subsequent API calls. */
    private UUID id;

    /**
     * Human-readable business code, e.g. "TL-2025-0001".
     * This is the code that appears on the printed Biên bản thanh lý.
     */
    private String requestCode;

    /** UUID of the asset being liquidated. */
    private UUID assetId;

    /** UUID of the unit that requested the liquidation. */
    private UUID requestingUnitId;

    /** Username of the person who created the request. */
    private String initiatedBy;

    // ── STATUS ────────────────────────────────────────────────────────────

    /**
     * Current workflow state.
     * Values: DRAFT | PENDING_MANAGER | PENDING_DIRECTOR | APPROVED | COMPLETED | REJECTED
     */
    private LiquidationStatus status;

    // ── HL-02 JUSTIFICATION FIELDS ────────────────────────────────────────

    /** The business justification for liquidating this asset. */
    private String justification;

    /** Asset physical condition: GOOD | FAIR | POOR | DAMAGED */
    private String assetCondition;

    /** Estimated current market value at request time (VND). */
    private BigDecimal currentMarketValue;

    /** Planned disposal method: AUCTION | SCRAP | DONATION */
    private String disposalMethod;

    /** Additional notes about the disposal plan. */
    private String disposalNotes;

    // ── STEP 1: MANAGER APPROVAL ──────────────────────────────────────────

    /** Username of the Step 1 approver. Null until acted on. */
    private String managerApprovedBy;

    /** Timestamp of Step 1 approval. */
    private LocalDateTime managerApprovedAt;

    /** Notes from the Step 1 approver. */
    private String managerNotes;

    // ── STEP 2: DIRECTOR APPROVAL ─────────────────────────────────────────

    /** Username of the Step 2 (director-level) approver. Null until acted on. */
    private String directorApprovedBy;

    /** Timestamp of Step 2 approval. */
    private LocalDateTime directorApprovedAt;

    /** Notes from the Step 2 approver. */
    private String directorNotes;

    // ── COMPLETION ────────────────────────────────────────────────────────

    /** Username of the person who triggered the COMPLETED transition. */
    private String completedBy;

    /** Timestamp when the workflow was closed. */
    private LocalDateTime completedAt;

    /**
     * Actual value realised from the disposal (may differ from currentMarketValue).
     * Provided at completion time.
     */
    private BigDecimal finalDisposalValue;

    // ── REJECTION ─────────────────────────────────────────────────────────

    /** Username of the person who rejected (at Step 1 or Step 2). */
    private String rejectedBy;

    /** Timestamp of rejection. */
    private LocalDateTime rejectedAt;

    /** Mandatory explanation for the rejection. */
    private String rejectionReason;

    // ── DOCUMENT (HL-03) ──────────────────────────────────────────────────

    /**
     * Reference number of the generated "Biên bản thanh lý tài sản" document.
     * Format: BBTL-YYYY-NNNN (derived from the TL-YYYY-NNNN request code).
     */
    private String documentRef;

    /** Timestamp when the document was generated. */
    private LocalDateTime documentGeneratedAt;

    /** Whether the document has been signed by all required parties. */
    private Boolean documentSigned;

    // ── AUDIT FIELDS (from BaseEntity) ────────────────────────────────────

    /** Timestamp when the request was first created. */
    private LocalDateTime createdAt;

    /** Timestamp of the most recent update to this record. */
    private LocalDateTime updatedAt;

    /** Username of whoever created the record (same as initiatedBy in practice). */
    private String createdBy;

    // ── FACTORY METHOD ────────────────────────────────────────────────────

    /**
     * Converts a LiquidationRequest JPA entity into this safe DTO.
     *
     * This is the single conversion point — always call this method rather than
     * manually constructing the DTO in service or controller code.
     * Mirrors the same pattern as HandoverDto.from() and AuditLogDto.from().
     *
     * @param entity The LiquidationRequest entity loaded from the database.
     * @return A fully populated LiquidationDto ready for the API response.
     */
    public static LiquidationDto from(LiquidationRequest entity) {
        return LiquidationDto.builder()
                // Identity
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .assetId(entity.getAssetId())
                .requestingUnitId(entity.getRequestingUnitId())
                .initiatedBy(entity.getInitiatedBy())
                // Status
                .status(entity.getStatus())
                // Justification
                .justification(entity.getJustification())
                .assetCondition(entity.getAssetCondition())
                .currentMarketValue(entity.getCurrentMarketValue())
                .disposalMethod(entity.getDisposalMethod())
                .disposalNotes(entity.getDisposalNotes())
                // Step 1
                .managerApprovedBy(entity.getManagerApprovedBy())
                .managerApprovedAt(entity.getManagerApprovedAt())
                .managerNotes(entity.getManagerNotes())
                // Step 2
                .directorApprovedBy(entity.getDirectorApprovedBy())
                .directorApprovedAt(entity.getDirectorApprovedAt())
                .directorNotes(entity.getDirectorNotes())
                // Completion
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
                .finalDisposalValue(entity.getFinalDisposalValue())
                // Rejection
                .rejectedBy(entity.getRejectedBy())
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                // Document
                .documentRef(entity.getDocumentRef())
                .documentGeneratedAt(entity.getDocumentGeneratedAt())
                .documentSigned(entity.getDocumentSigned())
                // Audit (from BaseEntity)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}