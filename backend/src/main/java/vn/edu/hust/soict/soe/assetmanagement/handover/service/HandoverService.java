package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * ==============================================================================
 * SERVICE: HandoverService
 * ==============================================================================
 * PURPOSE:
 *   Central business logic for the asset handover workflow (HL-01 to HL-03).
 *   This service is the ONLY component allowed to write to the `handover_requests`
 *   table. Controllers call this service; this service calls the repository.
 *
 * WORKFLOW MANAGED:
 *   DRAFT → PENDING_APPROVAL → APPROVED → CONFIRMED → COMPLETED
 *                                   └──────────────────────────► REJECTED
 *
 * CROSS-MODULE INTEGRATION:
 *   1. FixedAssetService  — called when a handover is APPROVED to:
 *                           a) Mark the asset as TRANSFERRED
 *                           b) Update asset.managingUnitId to the receiving unit
 *      Method used: fixedAssetService.updateAssetStatusAndUnit(assetId, TRANSFERRED, toUnitId, reason, username)
 *
 *   2. AuditLogService    — called on EVERY state transition to write a row
 *                           to audit_logs (module="HANDOVER"). This satisfies RP-03.
 *
 * KEY BUSINESS RULES ENFORCED HERE:
 *   Rule 1 — SEPARATION OF DUTIES:
 *     The person who submits (initiatedBy) CANNOT be the same person who
 *     approves at Step 1 (deptApprovedBy). Enforced in approveHandover().
 *
 *   Rule 2 — ONE ACTIVE REQUEST PER ASSET:
 *     If an asset already has a DRAFT/PENDING_APPROVAL/APPROVED/CONFIRMED request,
 *     no new request can be created for it. Enforced in createHandover().
 *
 *   Rule 3 — DIFFERENT UNITS:
 *     from_unit_id must not equal to_unit_id. Enforced in createHandover().
 *
 *   Rule 4 — VALID TRANSITIONS:
 *     Each workflow method checks the current status before proceeding.
 *     You cannot approve a COMPLETED request, cannot confirm a DRAFT, etc.
 *
 * TRANSACTION STRATEGY:
 *   All write methods are @Transactional (default: REQUIRED).
 *   If ANY step fails (including the AuditLog write, which uses REQUIRED
 *   propagation), the entire transaction rolls back — no phantom audit entries.
 *   Read-only methods use @Transactional(readOnly = true) for performance.
 *
 * CODE GENERATION:
 *   Request codes follow the format: BG-YYYY-NNNN (e.g. BG-2025-0042).
 *   Generated in generateRequestCode() using current year + sequential suffix
 *   based on total record count. Not strictly sequential under concurrent load,
 *   but good enough for this system's scale.
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HandoverService {

    private final HandoverRepository handoverRepository;
    private final FixedAssetService fixedAssetService;      // Cross-module: update asset status/unit
    private final AuditLogService auditLogService;           // Cross-module: write audit log
    private final HandoverDocumentService handoverDocumentService; // HL-03: generate document

    /**
     * Terminal statuses — used in the blocking-request query.
     * A request in COMPLETED or REJECTED state does NOT block new submissions.
     */
    private static final List<HandoverStatus> TERMINAL_STATUSES =
            List.of(HandoverStatus.COMPLETED, HandoverStatus.REJECTED);

    // ═══════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of ALL handover requests in the system.
     * Used by GET /api/handovers.
     *
     * Sorted newest-first by default (caller can override via Pageable).
     *
     * @param pageable Pagination/sorting parameters from the request query string.
     * @return Page of HandoverDto objects (entity never leaves service layer).
     */
    @Transactional(readOnly = true)
    public Page<HandoverDto> getAllHandovers(Pageable pageable) {
        return handoverRepository.findAll(pageable)
                .map(HandoverDto::from);
    }

    /**
     * Returns the full detail of a single handover request by its UUID.
     * Used by GET /api/handovers/{id}.
     *
     * @param id The UUID of the handover request.
     * @return The HandoverDto for the matching record.
     * @throws ResourceNotFoundException if no record with this ID exists.
     */
    @Transactional(readOnly = true)
    public HandoverDto getHandoverById(UUID id) {
        HandoverRequest request = findOrThrow(id);
        return HandoverDto.from(request);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 0: CREATE — DRAFT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new handover request in DRAFT status.
     * Used by POST /api/handovers.
     *
     * VALIDATION STEPS (in order):
     *   1. from_unit_id != to_unit_id  (Rule 3)
     *   2. No active request already exists for this asset  (Rule 2)
     *   3. The referenced asset exists (delegates to FixedAssetService — will throw
     *      ResourceNotFoundException if not found when approve() calls updateAsset)
     *
     * NOTE: We do NOT validate that fromUnitId matches the asset's current
     * managing unit here — that would create a tight coupling. The approver
     * is expected to verify this during the approval step. If needed, this
     * check can be added by calling fixedAssetService.getAssetById().
     *
     * @param request    The validated DTO from the request body.
     * @param initiatedBy The username extracted from the JWT token (never from the body).
     * @return HandoverDto of the newly created DRAFT request.
     * @throws BusinessRuleException if any validation rule is violated.
     */
    public HandoverDto createHandover(CreateHandoverRequest request, String initiatedBy) {

        // ── Rule 3: Sender and receiver must be different units ───────────
        if (request.getFromUnitId().equals(request.getToUnitId())) {
            throw new BusinessRuleException(
                    "Đơn vị bàn giao và đơn vị tiếp nhận không được trùng nhau.");
        }

        // ── Rule 2: No active (in-progress) request for this asset ────────
        if (handoverRepository.hasActiveRequestForAsset(request.getAssetId(), TERMINAL_STATUSES)) {
            throw new BusinessRuleException(
                    "Tài sản này đang có yêu cầu bàn giao chưa hoàn tất. " +
                    "Không thể tạo yêu cầu mới cho đến khi yêu cầu hiện tại kết thúc.");
        }

        // ── Build and persist the entity ──────────────────────────────────
        HandoverRequest entity = HandoverRequest.builder()
                .requestCode(generateRequestCode())
                .assetId(request.getAssetId())
                .fromUnitId(request.getFromUnitId())
                .toUnitId(request.getToUnitId())
                .initiatedBy(initiatedBy)
                .status(HandoverStatus.DRAFT)          // Always starts as DRAFT
                .reason(request.getReason())
                .handoverDate(request.getHandoverDate())
                .assetCondition(request.getAssetCondition())
                .notes(request.getNotes())
                .documentSigned(false)
                .build();

        HandoverRequest saved = handoverRepository.save(entity);
        log.info("Handover request created: {} by {}", saved.getRequestCode(), initiatedBy);

        // ── Audit log (RP-03) ─────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "CREATE",
                saved.getId().toString(),
                saved.getRequestCode(),
                null,
                "{\"status\": \"DRAFT\", \"assetId\": \"" + saved.getAssetId() + "\"}",
                "Tạo yêu cầu bàn giao " + saved.getRequestCode() +
                " cho tài sản " + saved.getAssetId()
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 0.5: SUBMIT — DRAFT → PENDING_APPROVAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Submits a DRAFT request for approval, transitioning to PENDING_APPROVAL.
     * Used by PUT /api/handovers/{id}/submit.
     *
     * Only the initiator or an admin should be able to submit their own draft.
     * Role enforcement is done at the controller level via @PreAuthorize.
     *
     * @param id       UUID of the handover request to submit.
     * @param username Username of the person submitting (from JWT).
     * @return Updated HandoverDto in PENDING_APPROVAL status.
     * @throws BusinessRuleException     if the request is not in DRAFT status.
     * @throws ResourceNotFoundException if no record with this ID exists.
     */
    public HandoverDto submitHandover(UUID id, String username) {
        HandoverRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, HandoverStatus.DRAFT,
                "Chỉ có thể nộp yêu cầu đang ở trạng thái DRAFT.");

        // ── Transition ────────────────────────────────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(HandoverStatus.PENDING_APPROVAL);
        HandoverRequest saved = handoverRepository.save(request);

        log.info("Handover {} submitted for approval by {}", saved.getRequestCode(), username);

        // ── Audit log ─────────────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "STATUS_CHANGE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"PENDING_APPROVAL\"}",
                "Yêu cầu bàn giao " + saved.getRequestCode() + " được nộp để phê duyệt"
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 1: APPROVE — PENDING_APPROVAL → APPROVED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Approves a handover request at Step 1 (department-head level).
     * Transitions: PENDING_APPROVAL → APPROVED.
     * Used by PUT /api/handovers/{id}/approve.
     *
     * SIDE EFFECT — ASSET STATUS UPDATE:
     *   When a handover is approved, the asset is immediately marked as TRANSFERRED
     *   and its managing unit is updated to the receiving unit. This is the
     *   cross-module call to FixedAssetService as documented in HL-01.
     *
     * SEPARATION OF DUTIES (Rule 1):
     *   The approver MUST be a different person from the initiator.
     *
     * @param id           UUID of the handover request to approve.
     * @param approverNotes Optional notes from the approver.
     * @param approvedBy   Username of the approver (from JWT — never from body).
     * @return Updated HandoverDto in APPROVED status.
     * @throws BusinessRuleException     if the request is not PENDING_APPROVAL or if
     *                                   approvedBy equals initiatedBy (separation of duties).
     * @throws ResourceNotFoundException if the request or the asset does not exist.
     */
    public HandoverDto approveHandover(UUID id, String approverNotes, String approvedBy) {
        HandoverRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, HandoverStatus.PENDING_APPROVAL,
                "Chỉ có thể phê duyệt yêu cầu đang ở trạng thái PENDING_APPROVAL.");

        // ── Rule 1: Separation of duties ──────────────────────────────────
        if (approvedBy.equals(request.getInitiatedBy())) {
            throw new BusinessRuleException(
                    "Người phê duyệt không được là người tạo yêu cầu. " +
                    "Vui lòng để người khác phê duyệt yêu cầu này.");
        }

        // ── Transition ────────────────────────────────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(HandoverStatus.APPROVED);
        request.setDeptApprovedBy(approvedBy);
        request.setDeptApprovedAt(LocalDateTime.now());
        request.setDeptApprovalNotes(approverNotes);
        HandoverRequest saved = handoverRepository.save(request);

        // ── Cross-module: Update the asset (HL-01) ────────────────────────
        // Mark the asset as TRANSFERRED and update its owning unit.
        // This call will also write its own entry to asset_history via FixedAssetService.
        fixedAssetService.updateAssetStatusAndUnit(
                saved.getAssetId(),
                AssetStatus.TRANSFERRED,
                saved.getToUnitId(),
                "Bàn giao tài sản theo yêu cầu " + saved.getRequestCode(),
                approvedBy
        );

        log.info("Handover {} approved by {}", saved.getRequestCode(), approvedBy);

        // ── Audit log (RP-03) ─────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "APPROVE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"APPROVED\", \"approvedBy\": \"" + approvedBy + "\"}",
                "Yêu cầu bàn giao " + saved.getRequestCode() + " được phê duyệt bởi " + approvedBy
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 2: CONFIRM — APPROVED → CONFIRMED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records that the receiving unit has physically confirmed receipt of the asset.
     * Transitions: APPROVED → CONFIRMED.
     * Used by PUT /api/handovers/{id}/confirm.
     *
     * In a real-world state SOE context this step represents the receiving unit
     * representative signing off that the asset physically arrived.
     *
     * @param id                UUID of the handover request.
     * @param confirmationNotes Optional notes from the receiving party.
     * @param confirmedBy       Username of the receiving-unit representative (from JWT).
     * @return Updated HandoverDto in CONFIRMED status.
     * @throws BusinessRuleException     if the request is not in APPROVED status.
     * @throws ResourceNotFoundException if the request does not exist.
     */
    public HandoverDto confirmHandover(UUID id, String confirmationNotes, String confirmedBy) {
        HandoverRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, HandoverStatus.APPROVED,
                "Chỉ có thể xác nhận bàn giao khi yêu cầu đã được phê duyệt (APPROVED).");

        // ── Transition ────────────────────────────────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(HandoverStatus.CONFIRMED);
        request.setConfirmedBy(confirmedBy);
        request.setConfirmedAt(LocalDateTime.now());
        request.setConfirmationNotes(confirmationNotes);
        HandoverRequest saved = handoverRepository.save(request);

        log.info("Handover {} confirmed by receiving unit: {}", saved.getRequestCode(), confirmedBy);

        // ── Audit log ─────────────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "CONFIRM",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"CONFIRMED\", \"confirmedBy\": \"" + confirmedBy + "\"}",
                "Đơn vị tiếp nhận xác nhận bàn giao " + saved.getRequestCode()
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 3: COMPLETE — CONFIRMED → COMPLETED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Closes the handover workflow after the formal document is generated.
     * Transitions: CONFIRMED → COMPLETED.
     * Used by PUT /api/handovers/{id}/complete.
     *
     * This step also triggers document generation via HandoverDocumentService (HL-03).
     * After completion, the request is permanently closed — no further transitions.
     *
     * @param id          UUID of the handover request.
     * @param completedBy Username of the person finalizing the record (from JWT).
     * @return Updated HandoverDto in COMPLETED status.
     * @throws BusinessRuleException     if the request is not in CONFIRMED status.
     * @throws ResourceNotFoundException if the request does not exist.
     */
    public HandoverDto completeHandover(UUID id, String completedBy) {
        HandoverRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, HandoverStatus.CONFIRMED,
                "Chỉ có thể hoàn tất khi yêu cầu đã được xác nhận (CONFIRMED).");

        // ── HL-03: Generate the formal handover document ──────────────────
        // HandoverDocumentService generates the "Biên bản bàn giao" and returns
        // the reference number for the document record.
        String documentRef = handoverDocumentService.generateDocument(request);
        request.setDocumentRef(documentRef);
        request.setDocumentGeneratedAt(LocalDateTime.now());

        // ── Transition ────────────────────────────────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(HandoverStatus.COMPLETED);
        request.setCompletedBy(completedBy);
        request.setCompletedAt(LocalDateTime.now());
        HandoverRequest saved = handoverRepository.save(request);

        log.info("Handover {} completed by {}. Document ref: {}",
                saved.getRequestCode(), completedBy, documentRef);

        // ── Audit log (RP-03) ─────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "COMPLETE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"COMPLETED\", \"documentRef\": \"" + documentRef + "\"}",
                "Hoàn tất bàn giao " + saved.getRequestCode() +
                ". Biên bản: " + documentRef
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REJECT — can happen from PENDING_APPROVAL or APPROVED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rejects a handover request at any in-progress step.
     * Transitions: PENDING_APPROVAL or APPROVED → REJECTED.
     * Used by PUT /api/handovers/{id}/reject.
     *
     * IMPORTANT — ASSET ROLLBACK:
     *   If the request was already in APPROVED status (meaning the asset was
     *   already marked TRANSFERRED), rejecting must roll back the asset's status
     *   to IN_USE and restore the original managing unit.
     *
     * @param id             UUID of the handover request to reject.
     * @param rejectionReason Mandatory text explaining why it was rejected.
     * @param rejectedBy     Username of the rejector (from JWT).
     * @return Updated HandoverDto in REJECTED status.
     * @throws BusinessRuleException     if the request is already in a terminal state,
     *                                   or if rejectionReason is blank.
     * @throws ResourceNotFoundException if the request does not exist.
     */
    public HandoverDto rejectHandover(UUID id, String rejectionReason, String rejectedBy) {
        HandoverRequest request = findOrThrow(id);

        // ── Validate that the request can still be rejected ───────────────
        if (request.getStatus() == HandoverStatus.COMPLETED ||
                request.getStatus() == HandoverStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Không thể từ chối yêu cầu đã ở trạng thái " +
                    request.getStatus().name() + ".");
        }
        if (request.getStatus() == HandoverStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "Không thể từ chối yêu cầu đã được đơn vị tiếp nhận xác nhận (CONFIRMED). " +
                    "Vui lòng liên hệ quản trị viên.");
        }

        // ── Validate rejection reason is provided ─────────────────────────
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessRuleException("Lý do từ chối không được để trống.");
        }

        // ── If APPROVED: roll back the asset status to IN_USE ─────────────
        // The asset was marked TRANSFERRED when approved. Rejection undoes this.
        String oldStatus = request.getStatus().name();
        if (request.getStatus() == HandoverStatus.APPROVED) {
            fixedAssetService.updateAssetStatusAndUnit(
                    request.getAssetId(),
                    AssetStatus.IN_USE,
                    request.getFromUnitId(),   // Restore original unit
                    "Hoàn trả trạng thái tài sản do yêu cầu bàn giao " +
                    request.getRequestCode() + " bị từ chối",
                    rejectedBy
            );
        }

        // ── Transition ────────────────────────────────────────────────────
        request.setStatus(HandoverStatus.REJECTED);
        request.setRejectedBy(rejectedBy);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        HandoverRequest saved = handoverRepository.save(request);

        log.info("Handover {} rejected by {}. Reason: {}",
                saved.getRequestCode(), rejectedBy, rejectionReason);

        // ── Audit log (RP-03) ─────────────────────────────────────────────
        auditLogService.log(
                "HANDOVER",
                "REJECT",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"REJECTED\", \"rejectedBy\": \"" + rejectedBy + "\"}",
                "Từ chối yêu cầu bàn giao " + saved.getRequestCode() +
                " bởi " + rejectedBy + ". Lý do: " + rejectionReason
        );

        return HandoverDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches a HandoverRequest by ID, or throws ResourceNotFoundException.
     * Centralizes the error message to keep all methods consistent.
     *
     * @param id The UUID to look up.
     * @return The found entity.
     * @throws ResourceNotFoundException with a descriptive message.
     */
    private HandoverRequest findOrThrow(UUID id) {
        return handoverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu bàn giao với ID: " + id));
    }

    /**
     * Validates that the given request is currently in the expected status.
     * If it's not, throws BusinessRuleException with the provided message.
     *
     * This helper exists so every workflow method has a single, readable
     * precondition check at the top.
     *
     * @param request        The entity to check.
     * @param expectedStatus The status that is required for this operation.
     * @param errorMessage   The Vietnamese error message to return if the check fails.
     * @throws BusinessRuleException if the request is not in expectedStatus.
     */
    private void requireStatus(HandoverRequest request,
                                HandoverStatus expectedStatus,
                                String errorMessage) {
        if (request.getStatus() != expectedStatus) {
            throw new BusinessRuleException(
                    errorMessage + " Trạng thái hiện tại: " + request.getStatus().name());
        }
    }

    /**
     * Generates a unique, human-readable request code in the format BG-YYYY-NNNN.
     * Example: "BG-2025-0001", "BG-2025-0042"
     *
     * Strategy:
     *   - "BG" = Biên bản bàn Giao (official Vietnamese abbreviation)
     *   - YYYY = current year
     *   - NNNN = total count of existing records + 1, zero-padded to 4 digits
     *
     * NOTE: Under high concurrent load (many simultaneous requests), two threads
     * could generate the same code. The `request_code UNIQUE` constraint in the DB
     * will catch this and cause a DataIntegrityViolationException. For this system's
     * scale (internal SOE tool), this is acceptable. If needed, a database sequence
     * or UUID-based code can be used instead.
     *
     * @return A new unique request code string.
     */
    private String generateRequestCode() {
        String year = String.valueOf(java.time.Year.now().getValue());
        long count = handoverRepository.count() + 1;
        return String.format("BG-%s-%04d", year, count);
    }
}