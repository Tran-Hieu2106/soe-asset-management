package vn.edu.hust.soict.soe.assetmanagement.liquidation.service;

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
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.CreateLiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.LiquidationDto;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.repository.LiquidationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ==============================================================================
 * SERVICE: LiquidationService
 * ==============================================================================
 * PURPOSE:
 *   Central business logic for the asset liquidation workflow (HL-02, HL-02a, HL-03).
 *   This service is the ONLY component allowed to write to `liquidation_requests`.
 *   All controllers call this service; this service calls the repository.
 *
 * WORKFLOW MANAGED:
 *   DRAFT → PENDING_MANAGER → PENDING_DIRECTOR → APPROVED → COMPLETED
 *                   └──────────────────────────────────────► REJECTED
 *
 * CRITICAL DIFFERENCE FROM HANDOVER MODULE:
 *   In the handover module, the asset is modified (marked TRANSFERRED) when the
 *   request is APPROVED. Here, the asset is NOT touched until the workflow reaches
 *   COMPLETED — only then is the asset permanently set to LIQUIDATED.
 *   This means there is NO asset rollback needed on rejection at any step.
 *
 * CROSS-MODULE INTEGRATION:
 *   1. FixedAssetService — called ONLY at COMPLETED to:
 *        a) Set the asset status to LIQUIDATED (permanently, BR-05)
 *        b) This also writes to asset_history via FixedAssetService
 *      Method used: fixedAssetService.updateAssetStatus(assetId, LIQUIDATED, reason, username)
 *
 *   2. AuditLogService — called on EVERY state transition to write to audit_logs.
 *      Required by RP-01. Uses Propagation.REQUIRED so audit log writes are part
 *      of the same transaction as the liquidation request update.
 *
 * KEY BUSINESS RULES ENFORCED:
 *   BR-02 — SEPARATION OF DUTIES:
 *     The initiator (initiatedBy) must not be the same person as the Step 1
 *     approver (managerApprovedBy). Enforced in approveManagerStep().
 *     Note: Step 2 (director) has no separation-of-duties constraint with Step 1
 *     because they represent different organisational authority levels.
 *
 *   ONE ACTIVE REQUEST PER ASSET:
 *     If an asset already has a DRAFT / PENDING_MANAGER / PENDING_DIRECTOR / APPROVED
 *     request, no new one can be created. Enforced in createLiquidation().
 *
 *   BR-05 — LIQUIDATED ASSET IMMUTABILITY:
 *     Once the workflow completes and the asset is set to LIQUIDATED, no further
 *     modifications to the asset are permitted. This rule is enforced inside
 *     FixedAssetService (the asset module checks for LIQUIDATED status before
 *     allowing any update) and at the service layer for liquidation by blocking
 *     further workflow transitions after COMPLETED.
 *
 * TRANSACTION STRATEGY:
 *   All write methods are @Transactional (default Propagation.REQUIRED).
 *   If any step fails — including the AuditLogService call — the entire
 *   operation rolls back. No "phantom" approvals or partial updates.
 *   Read-only methods use @Transactional(readOnly = true) for performance.
 *
 * DOCUMENT NUMBERING (BR-07):
 *   Request codes use format: TL-YYYY-NNNN  ("TL" = Thanh Lý)
 *   Document refs use format: BBTL-YYYY-NNNN ("BBTL" = Biên Bản Thanh Lý)
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LiquidationService {

    private final LiquidationRepository liquidationRepository;
    private final FixedAssetService fixedAssetService;   // Cross-module: set asset to LIQUIDATED
    private final AuditLogService auditLogService;        // Cross-module: write audit log

    /**
     * Terminal statuses — requests in these states do NOT block new submissions
     * for the same asset. Declared as a constant so the business rule is in
     * exactly one place and easy to read.
     */
    private static final List<LiquidationStatus> TERMINAL_STATUSES =
            List.of(LiquidationStatus.COMPLETED, LiquidationStatus.REJECTED);

    // ═══════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of ALL liquidation requests in the system.
     * Used by GET /api/liquidations.
     *
     * @param pageable Pagination/sorting parameters (typically createdAt DESC).
     * @return Page of LiquidationDto objects. Entity never leaves service layer.
     */
    @Transactional(readOnly = true)
    public Page<LiquidationDto> getAllLiquidations(Pageable pageable) {
        return liquidationRepository.findAll(pageable)
                .map(LiquidationDto::from);
    }

    /**
     * Returns the full detail of a single liquidation request.
     * Used by GET /api/liquidations/{id}.
     *
     * @param id UUID of the liquidation request.
     * @return LiquidationDto with all fields populated.
     * @throws ResourceNotFoundException if no request with this ID exists (→ 404).
     */
    @Transactional(readOnly = true)
    public LiquidationDto getLiquidationById(UUID id) {
        LiquidationRequest request = findOrThrow(id);
        return LiquidationDto.from(request);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 0: CREATE — DRAFT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new liquidation request in DRAFT status.
     * Used by POST /api/liquidations.
     *
     * VALIDATION (in order):
     *   1. No active in-progress request for the same asset already exists.
     *   2. The asset itself exists (implicitly verified — FixedAssetService will
     *      throw ResourceNotFoundException at complete time if it disappeared).
     *
     * NOTE ON initiatedBy:
     *   The username is passed in from the controller, which reads it from
     *   authentication.getName() (JWT token). It is never read from the request body.
     *
     * @param request     Validated DTO from the POST body.
     * @param initiatedBy Username from the JWT token.
     * @return LiquidationDto of the newly created DRAFT request.
     * @throws BusinessRuleException if an active request already exists for the asset.
     */
    public LiquidationDto createLiquidation(CreateLiquidationRequest request,
                                             String initiatedBy) {

        // ── Block duplicate active requests for the same asset ────────────
        if (liquidationRepository.hasActiveRequestForAsset(
                request.getAssetId(), TERMINAL_STATUSES)) {
            throw new BusinessRuleException(
                    "Tài sản này đang có yêu cầu thanh lý chưa hoàn tất. " +
                    "Không thể tạo yêu cầu mới cho đến khi yêu cầu hiện tại kết thúc.");
        }

        // ── Build the entity ──────────────────────────────────────────────
        // status always starts as DRAFT; documentSigned defaults to false
        LiquidationRequest entity = LiquidationRequest.builder()
                .requestCode(generateRequestCode())
                .assetId(request.getAssetId())
                .requestingUnitId(request.getRequestingUnitId())
                .initiatedBy(initiatedBy)
                .status(LiquidationStatus.DRAFT)
                .justification(request.getJustification())
                .assetCondition(request.getAssetCondition())
                .currentMarketValue(request.getCurrentMarketValue())
                .disposalMethod(request.getDisposalMethod())
                .disposalNotes(request.getDisposalNotes())
                .documentSigned(false)
                .build();

        LiquidationRequest saved = liquidationRepository.save(entity);
        log.info("Liquidation request created: {} by {}", saved.getRequestCode(), initiatedBy);

        // ── Audit log (RP-01) ─────────────────────────────────────────────
        // oldValue = null for CREATE events (per RP-01 specification)
        auditLogService.log(
                "LIQUIDATION",
                "CREATE",
                saved.getId().toString(),
                saved.getRequestCode(),
                null,
                "{\"status\": \"DRAFT\", \"assetId\": \"" + saved.getAssetId() +
                "\", \"disposalMethod\": \"" + saved.getDisposalMethod() + "\"}",
                "Tạo yêu cầu thanh lý " + saved.getRequestCode() +
                " cho tài sản " + saved.getAssetId()
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 0.5: SUBMIT — DRAFT → PENDING_MANAGER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Submits a DRAFT request for Step 1 review, transitioning to PENDING_MANAGER.
     * Used by PUT /api/liquidations/{id}/submit.
     *
     * Only the initiator or an admin should submit a draft.
     * Role enforcement is at the controller level via @PreAuthorize.
     *
     * @param id       UUID of the request to submit.
     * @param username JWT username of the submitter.
     * @return Updated LiquidationDto in PENDING_MANAGER status.
     * @throws BusinessRuleException     if request is not in DRAFT status.
     * @throws ResourceNotFoundException if no request with this ID exists.
     */
    public LiquidationDto submitLiquidation(UUID id, String username) {
        LiquidationRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, LiquidationStatus.DRAFT,
                "Chỉ có thể nộp yêu cầu đang ở trạng thái DRAFT.");

        // ── Transition: DRAFT → PENDING_MANAGER ───────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(LiquidationStatus.PENDING_MANAGER);
        LiquidationRequest saved = liquidationRepository.save(request);

        log.info("Liquidation {} submitted for manager review by {}",
                saved.getRequestCode(), username);

        // ── Audit log ─────────────────────────────────────────────────────
        auditLogService.log(
                "LIQUIDATION",
                "STATUS_CHANGE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"PENDING_MANAGER\"}",
                "Yêu cầu thanh lý " + saved.getRequestCode() +
                " được nộp để xét duyệt cấp quản lý"
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 1: MANAGER APPROVAL — PENDING_MANAGER → PENDING_DIRECTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records Step 1 approval (Asset Manager / department-head level).
     * Transitions: PENDING_MANAGER → PENDING_DIRECTOR.
     * Used by PUT /api/liquidations/{id}/approve-manager.
     *
     * SEPARATION OF DUTIES (BR-02):
     *   The Step 1 approver MUST be a different person from the initiator.
     *   The system enforces this at the service layer — it cannot be bypassed
     *   by any role, including SYSTEM_ADMIN.
     *
     * NOTE ON ASSET:
     *   The asset table is NOT touched here. The asset is only modified at
     *   COMPLETED. This is the key difference from the handover module.
     *
     * @param id            UUID of the request to approve.
     * @param approverNotes Optional notes from the approver.
     * @param approvedBy    JWT username of the approver.
     * @return Updated LiquidationDto in PENDING_DIRECTOR status.
     * @throws BusinessRuleException if not PENDING_MANAGER, or approvedBy equals initiatedBy.
     */
    public LiquidationDto approveManagerStep(UUID id, String approverNotes,
                                              String approvedBy) {
        LiquidationRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, LiquidationStatus.PENDING_MANAGER,
                "Chỉ có thể phê duyệt cấp quản lý khi yêu cầu đang ở trạng thái PENDING_MANAGER.");

        // ── BR-02: Separation of duties ───────────────────────────────────
        // The person who created the request cannot be the one who approves it
        // at Step 1. This is a hard business rule enforced regardless of role.
        if (approvedBy.equals(request.getInitiatedBy())) {
            throw new BusinessRuleException(
                    "Người phê duyệt cấp quản lý không được là người tạo yêu cầu. " +
                    "Vui lòng để người khác phê duyệt yêu cầu này.");
        }

        // ── Transition: PENDING_MANAGER → PENDING_DIRECTOR ────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(LiquidationStatus.PENDING_DIRECTOR);
        request.setManagerApprovedBy(approvedBy);
        request.setManagerApprovedAt(LocalDateTime.now());
        request.setManagerNotes(approverNotes);
        LiquidationRequest saved = liquidationRepository.save(request);

        log.info("Liquidation {} approved at manager level by {}",
                saved.getRequestCode(), approvedBy);

        // ── Audit log (RP-01) ─────────────────────────────────────────────
        auditLogService.log(
                "LIQUIDATION",
                "APPROVE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"PENDING_DIRECTOR\", \"managerApprovedBy\": \"" +
                approvedBy + "\"}",
                "Yêu cầu thanh lý " + saved.getRequestCode() +
                " được phê duyệt cấp quản lý bởi " + approvedBy
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 2: DIRECTOR APPROVAL — PENDING_DIRECTOR → APPROVED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records Step 2 approval (Director / board level).
     * Transitions: PENDING_DIRECTOR → APPROVED.
     * Used by PUT /api/liquidations/{id}/approve-director.
     *
     * After this step, the request is fully authorised for disposal.
     * The final COMPLETED transition (which actually modifies the asset)
     * happens in a separate step to allow for scheduling and documentation.
     *
     * NOTE: No separation-of-duties check between Step 1 and Step 2 approvers,
     * as they represent different organisational authority levels. The constraint
     * is only between the initiator and the Step 1 approver (BR-02).
     *
     * @param id            UUID of the request.
     * @param approverNotes Optional notes from the director.
     * @param approvedBy    JWT username of the director approver.
     * @return Updated LiquidationDto in APPROVED status.
     * @throws BusinessRuleException if not PENDING_DIRECTOR.
     */
    public LiquidationDto approveDirectorStep(UUID id, String approverNotes,
                                               String approvedBy) {
        LiquidationRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, LiquidationStatus.PENDING_DIRECTOR,
                "Chỉ có thể phê duyệt cấp giám đốc khi yêu cầu đang ở trạng thái PENDING_DIRECTOR.");

        // ── Transition: PENDING_DIRECTOR → APPROVED ───────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(LiquidationStatus.APPROVED);
        request.setDirectorApprovedBy(approvedBy);
        request.setDirectorApprovedAt(LocalDateTime.now());
        request.setDirectorNotes(approverNotes);
        LiquidationRequest saved = liquidationRepository.save(request);

        log.info("Liquidation {} approved at director level by {}",
                saved.getRequestCode(), approvedBy);

        // ── Audit log ─────────────────────────────────────────────────────
        auditLogService.log(
                "LIQUIDATION",
                "APPROVE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"APPROVED\", \"directorApprovedBy\": \"" +
                approvedBy + "\"}",
                "Yêu cầu thanh lý " + saved.getRequestCode() +
                " được phê duyệt cấp giám đốc bởi " + approvedBy
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STEP 3: COMPLETE — APPROVED → COMPLETED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Finalises the liquidation workflow. Transitions: APPROVED → COMPLETED.
     * Used by PUT /api/liquidations/{id}/complete.
     *
     * THIS IS THE MOST CRITICAL METHOD IN THIS MODULE. It:
     *   1. Validates the request is in APPROVED state.
     *   2. Sets the final disposal value on the request record.
     *   3. Generates the "Biên bản thanh lý" document reference (HL-03).
     *   4. Makes the cross-module call to permanently set the asset to LIQUIDATED.
     *   5. Updates the request to COMPLETED.
     *   6. Writes the audit log.
     *
     * All 6 operations happen in one @Transactional context. If any fails,
     * everything rolls back — no partial state (HL-02a atomic requirement).
     *
     * ASSET IMMUTABILITY (BR-05):
     *   After this call, the asset is permanently LIQUIDATED. No further changes
     *   to the asset's profile, status, or financial parameters are permitted.
     *   FixedAssetService enforces this rule by checking asset status before
     *   allowing any modification.
     *
     * @param id                 UUID of the request to complete.
     * @param finalDisposalValue The actual value realised from disposal (VND). Optional.
     * @param completedBy        JWT username of the person finalising the record.
     * @return Updated LiquidationDto in COMPLETED status.
     * @throws BusinessRuleException     if the request is not in APPROVED status.
     * @throws ResourceNotFoundException if the request or asset does not exist.
     */
    public LiquidationDto completeLiquidation(UUID id, BigDecimal finalDisposalValue,
                                               String completedBy) {
        LiquidationRequest request = findOrThrow(id);

        // ── Validate current state ────────────────────────────────────────
        requireStatus(request, LiquidationStatus.APPROVED,
                "Chỉ có thể hoàn tất thanh lý khi yêu cầu đã được phê duyệt (APPROVED).");

        // ── Set final disposal value on the request ───────────────────────
        // This is the actual money received from auction/scrap/donation.
        // It may differ from currentMarketValue (the estimate at request time).
        request.setFinalDisposalValue(finalDisposalValue);

        // ── Generate document reference (HL-03) ───────────────────────────
        // Convert "TL-2025-0001" → "BBTL-2025-0001" (Biên Bản Thanh Lý)
        // In production this would render a Thymeleaf template and generate a PDF.
        String documentRef = request.getRequestCode().replace("TL-", "BBTL-");
        request.setDocumentRef(documentRef);
        request.setDocumentGeneratedAt(LocalDateTime.now());

        log.info("Generating liquidation document {} for request {}",
                documentRef, request.getRequestCode());

        // ── Transition: APPROVED → COMPLETED ─────────────────────────────
        String oldStatus = request.getStatus().name();
        request.setStatus(LiquidationStatus.COMPLETED);
        request.setCompletedBy(completedBy);
        request.setCompletedAt(LocalDateTime.now());
        LiquidationRequest saved = liquidationRepository.save(request);

        // ── Cross-module: Permanently set asset to LIQUIDATED (HL-02a) ────
        // This call writes to the assets table AND appends to asset_history.
        // After this, the asset is permanently read-only (BR-05).
        // The updateAssetStatus() method is used (no unit change needed for liquidation).
        fixedAssetService.updateAssetStatus(
                saved.getAssetId(),
                AssetStatus.LIQUIDATED,
                "Tài sản bị thanh lý theo biên bản " + documentRef +
                " (Yêu cầu " + saved.getRequestCode() + ")",
                completedBy
        );

        log.info("Liquidation {} completed by {}. Asset {} set to LIQUIDATED. Document: {}",
                saved.getRequestCode(), completedBy, saved.getAssetId(), documentRef);

        // ── Audit log (RP-01) ─────────────────────────────────────────────
        auditLogService.log(
                "LIQUIDATION",
                "COMPLETE",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"COMPLETED\", \"documentRef\": \"" + documentRef +
                "\", \"finalDisposalValue\": " + finalDisposalValue + "}",
                "Hoàn tất thanh lý " + saved.getRequestCode() +
                ". Biên bản: " + documentRef +
                ". Tài sản " + saved.getAssetId() + " đã bị thanh lý."
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REJECT — can happen from PENDING_MANAGER or PENDING_DIRECTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rejects a liquidation request at any in-progress step.
     * Can reject from: PENDING_MANAGER or PENDING_DIRECTOR.
     * Cannot reject from: DRAFT (not yet submitted), APPROVED, COMPLETED, or REJECTED.
     * Used by PUT /api/liquidations/{id}/reject.
     *
     * KEY DIFFERENCE FROM HANDOVER:
     *   In the handover module, rejecting after APPROVED requires rolling back
     *   the asset status. Here, there is NO asset rollback needed because
     *   the asset is never touched until COMPLETED. Rejection is simple —
     *   just record the rejection details and close the workflow.
     *
     * @param id              UUID of the request to reject.
     * @param rejectionReason Mandatory explanation for the rejection.
     * @param rejectedBy      JWT username of the person rejecting.
     * @return Updated LiquidationDto in REJECTED status.
     * @throws BusinessRuleException if the request is not rejectable, or reason is blank.
     */
    public LiquidationDto rejectLiquidation(UUID id, String rejectionReason,
                                             String rejectedBy) {
        LiquidationRequest request = findOrThrow(id);

        // ── Validate that the request can still be rejected ───────────────
        // DRAFT: not yet submitted — cannot reject what hasn't been submitted
        if (request.getStatus() == LiquidationStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Không thể từ chối yêu cầu đang ở trạng thái DRAFT. " +
                    "Yêu cầu phải được nộp trước khi có thể từ chối.");
        }
        // APPROVED: already past both approvals — only completion is allowed now
        if (request.getStatus() == LiquidationStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Không thể từ chối yêu cầu đã ở trạng thái APPROVED. " +
                    "Yêu cầu đã được phê duyệt đầy đủ, chỉ có thể hoàn tất.");
        }
        // Terminal states: already done
        if (request.getStatus() == LiquidationStatus.COMPLETED ||
                request.getStatus() == LiquidationStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Không thể từ chối yêu cầu đã ở trạng thái " +
                    request.getStatus().name() + ".");
        }

        // ── Validate rejection reason is provided ─────────────────────────
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessRuleException("Lý do từ chối không được để trống.");
        }

        // ── Transition: PENDING_MANAGER or PENDING_DIRECTOR → REJECTED ────
        // No asset rollback needed — asset was never modified during this workflow.
        String oldStatus = request.getStatus().name();
        request.setStatus(LiquidationStatus.REJECTED);
        request.setRejectedBy(rejectedBy);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        LiquidationRequest saved = liquidationRepository.save(request);

        log.info("Liquidation {} rejected by {} from status {}. Reason: {}",
                saved.getRequestCode(), rejectedBy, oldStatus, rejectionReason);

        // ── Audit log ─────────────────────────────────────────────────────
        auditLogService.log(
                "LIQUIDATION",
                "REJECT",
                saved.getId().toString(),
                saved.getRequestCode(),
                "{\"status\": \"" + oldStatus + "\"}",
                "{\"status\": \"REJECTED\", \"rejectedBy\": \"" + rejectedBy + "\"}",
                "Từ chối yêu cầu thanh lý " + saved.getRequestCode() +
                " bởi " + rejectedBy + ". Lý do: " + rejectionReason
        );

        return LiquidationDto.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches a LiquidationRequest by ID, or throws ResourceNotFoundException.
     * Centralises error handling so all workflow methods show a consistent message.
     *
     * @param id The UUID to look up.
     * @return The found entity.
     * @throws ResourceNotFoundException with a Vietnamese error message → HTTP 404.
     */
    private LiquidationRequest findOrThrow(UUID id) {
        return liquidationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu thanh lý với ID: " + id));
    }

    /**
     * Checks that the given request is currently in the expected status.
     * Throws BusinessRuleException if not, appending the current status to the message
     * so the caller knows what state the request is actually in.
     *
     * This helper prevents duplicated status-check code across all workflow methods.
     *
     * @param request        The entity whose status to check.
     * @param expectedStatus The status required for the calling operation.
     * @param errorMessage   Vietnamese error message shown if the check fails.
     * @throws BusinessRuleException → HTTP 400 if status does not match.
     */
    private void requireStatus(LiquidationRequest request,
                                LiquidationStatus expectedStatus,
                                String errorMessage) {
        if (request.getStatus() != expectedStatus) {
            throw new BusinessRuleException(
                    errorMessage + " Trạng thái hiện tại: " + request.getStatus().name());
        }
    }

    /**
     * Generates a unique human-readable request code in the format TL-YYYY-NNNN.
     * Examples: "TL-2025-0001", "TL-2025-0042"
     *
     * Format breakdown:
     *   "TL"   = Thanh Lý (Vietnamese for "Liquidation")
     *   YYYY   = current calendar year
     *   NNNN   = total existing record count + 1, zero-padded to 4 digits
     *
     * Concurrency note: two simultaneous requests could theoretically generate
     * the same code. The UNIQUE constraint on request_code in the DB catches this
     * and throws DataIntegrityViolationException. At SOE scale this is acceptable.
     * If needed, replace with a database sequence for strict uniqueness.
     *
     * Per BR-07 (SRS §5.5): liquidation documents use "TL-YYYY-NNN" numbering.
     *
     * @return A new request code string.
     */
    private String generateRequestCode() {
        String year = String.valueOf(java.time.Year.now().getValue());
        long count = liquidationRepository.count() + 1;
        return String.format("TL-%s-%04d", year, count);
    }
}