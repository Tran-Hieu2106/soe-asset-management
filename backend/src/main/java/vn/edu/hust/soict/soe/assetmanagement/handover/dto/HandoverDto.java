package vn.edu.hust.soict.soe.assetmanagement.handover.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * DTO: HandoverDto
 * ==============================================================================
 * PURPOSE:
 *   The read-side (response) representation of a HandoverRequest. This is the
 *   object returned by ALL handover endpoints (GET list, GET by ID, POST create,
 *   PUT approve, PUT reject). The JPA entity is never sent directly to the client.
 *
 * WHY A SEPARATE DTO:
 *   Per the project's architecture rules:
 *   "Never expose JPA entity objects directly in API responses — use DTOs."
 *   This keeps the API contract stable even if the entity changes internally,
 *   and prevents accidental exposure of lazy-loaded proxies or Hibernate metadata.
 *
 * MAPPING:
 *   Use the static factory method HandoverDto.from(HandoverRequest entity) to
 *   convert. This is called inside HandoverService after every state change.
 *
 * FULL FIELD COVERAGE:
 *   Includes ALL columns from the `handover_requests` table so that the frontend
 *   can display the full audit trail of who did what at each step.
 *
 * USAGE EXAMPLE:
 *   HandoverDto dto = HandoverDto.from(savedRequest);
 *   return ResponseEntity.ok(ApiResponse.success("...", dto));
 * ==============================================================================
 */
@Getter
@Builder
public class HandoverDto {

    // ── IDENTITY ──────────────────────────────────────────────────────────

    /** Internal UUID — used by the frontend to reference this request in subsequent calls. */
    private UUID id;

    /** Human-readable business code (e.g. "BG-2025-001"). */
    private String requestCode;

    /** UUID of the asset being transferred. */
    private UUID assetId;

    // ── PARTIES ───────────────────────────────────────────────────────────

    /** UUID of the unit giving up the asset. */
    private UUID fromUnitId;

    /** UUID of the unit receiving the asset. */
    private UUID toUnitId;

    /** Username of the person who initiated this request. */
    private String initiatedBy;

    // ── STATUS ────────────────────────────────────────────────────────────

    /**
     * Current state of the workflow.
     * Possible values: DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, COMPLETED, REJECTED
     */
    private HandoverStatus status;

    // ── JUSTIFICATION ─────────────────────────────────────────────────────

    /** Business reason for the transfer. */
    private String reason;

    /** Date of the physical handover (planned or actual). */
    private LocalDate handoverDate;

    /** Asset condition at time of handover: GOOD | FAIR | POOR */
    private String assetCondition;

    /** Initiator's additional notes. */
    private String notes;

    // ── STEP 1: APPROVAL ──────────────────────────────────────────────────

    /** Username of the approver (Step 1). Null until step 1 is acted on. */
    private String deptApprovedBy;

    /** Timestamp of Step 1 approval. */
    private LocalDateTime deptApprovedAt;

    /** Notes from the Step 1 approver. */
    private String deptApprovalNotes;

    // ── STEP 2: CONFIRMATION ──────────────────────────────────────────────

    /** Username of the receiving-unit representative who confirmed (Step 2). */
    private String confirmedBy;

    /** Timestamp of Step 2 confirmation. */
    private LocalDateTime confirmedAt;

    /** Notes from the receiving unit at confirmation. */
    private String confirmationNotes;

    // ── STEP 3: COMPLETION ────────────────────────────────────────────────

    /** Username of the person who closed the workflow. */
    private String completedBy;

    /** Timestamp of COMPLETED status. */
    private LocalDateTime completedAt;

    // ── REJECTION ─────────────────────────────────────────────────────────

    /** Username of the person who rejected (at any step). */
    private String rejectedBy;

    /** Timestamp of rejection. */
    private LocalDateTime rejectedAt;

    /** Mandatory reason text when rejecting. */
    private String rejectionReason;

    // ── DOCUMENT (HL-03) ──────────────────────────────────────────────────

    /** Reference number of the generated "Biên bản bàn giao" document. */
    private String documentRef;

    /** Timestamp when the document was generated. */
    private LocalDateTime documentGeneratedAt;

    /** Whether the document has been signed. */
    private Boolean documentSigned;

    // ── AUDIT FIELDS (from BaseEntity) ────────────────────────────────────

    /** Timestamp when the request was first created. */
    private LocalDateTime createdAt;

    /** Timestamp of the last update to this record. */
    private LocalDateTime updatedAt;

    /** Username of whoever created the record (same as initiatedBy in practice). */
    private String createdBy;

    // ── FACTORY METHOD ────────────────────────────────────────────────────

    /**
     * Converts a HandoverRequest JPA entity into this safe DTO.
     *
     * This is the single conversion point — always use this method rather than
     * building the DTO manually, to ensure all fields stay in sync if the
     * entity is updated.
     *
     * @param entity The HandoverRequest entity loaded from the database.
     * @return A fully populated HandoverDto ready for the API response.
     */
    public static HandoverDto from(HandoverRequest entity) {
        return HandoverDto.builder()
                // Identity
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .assetId(entity.getAssetId())
                // Parties
                .fromUnitId(entity.getFromUnitId())
                .toUnitId(entity.getToUnitId())
                .initiatedBy(entity.getInitiatedBy())
                // Status
                .status(entity.getStatus())
                // Justification
                .reason(entity.getReason())
                .handoverDate(entity.getHandoverDate())
                .assetCondition(entity.getAssetCondition())
                .notes(entity.getNotes())
                // Step 1
                .deptApprovedBy(entity.getDeptApprovedBy())
                .deptApprovedAt(entity.getDeptApprovedAt())
                .deptApprovalNotes(entity.getDeptApprovalNotes())
                // Step 2
                .confirmedBy(entity.getConfirmedBy())
                .confirmedAt(entity.getConfirmedAt())
                .confirmationNotes(entity.getConfirmationNotes())
                // Step 3
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
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