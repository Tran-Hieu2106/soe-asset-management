package vn.edu.hust.soict.soe.assetmanagement.handover.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.common.ApiResponse;
import vn.edu.hust.soict.soe.assetmanagement.common.PageResponse;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.service.HandoverService;

import java.util.UUID;

/**
 * ==============================================================================
 * CONTROLLER: HandoverController
 * ==============================================================================
 * PURPOSE:
 *   Exposes the REST API for the asset handover workflow (HL-01, HL-03).
 *   This controller is a thin HTTP adapter — it handles request parsing,
 *   authentication extraction, and response wrapping. ALL business logic
 *   lives in HandoverService.
 *
 * ARCHITECTURE RULES FOLLOWED:
 *   1. Every response is wrapped in ApiResponse<T>   (project standard)
 *   2. List responses use PageResponse<T>            (prevents large response bodies)
 *   3. Username is ALWAYS extracted from the Authentication object (JWT-provided)
 *      and NEVER read from the request body (security rule — prevents impersonation)
 *   4. No try/catch blocks — GlobalExceptionHandler handles all exceptions
 *   5. No business logic — all validation and processing is in HandoverService
 *
 * SECURITY (from SecurityConfig.java):
 *   /api/handovers/** → hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')
 *   Additional @PreAuthorize annotations below restrict specific operations:
 *     - Creating and submitting  → ASSET_MANAGER (the person who manages assets)
 *     - Approving and rejecting  → APPROVING_AUTH (the designated approver)
 *     - Confirming               → APPROVING_AUTH or ASSET_MANAGER
 *     - Completing               → SYSTEM_ADMIN or ASSET_MANAGER
 *     - Reading                  → all three roles
 *
 * API ENDPOINTS:
 *   GET    /api/handovers              → list all (paginated)
 *   GET    /api/handovers/{id}         → get one by ID
 *   POST   /api/handovers              → create new (DRAFT)
 *   PUT    /api/handovers/{id}/submit  → DRAFT → PENDING_APPROVAL
 *   PUT    /api/handovers/{id}/approve → PENDING_APPROVAL → APPROVED
 *   PUT    /api/handovers/{id}/confirm → APPROVED → CONFIRMED
 *   PUT    /api/handovers/{id}/complete→ CONFIRMED → COMPLETED
 *   PUT    /api/handovers/{id}/reject  → (any active state) → REJECTED
 *
 * NOTE ON REJECT BODY:
 *   The reject endpoint accepts an optional `reason` query parameter rather than
 *   a full request body, to keep simple rejection flows lightweight. HandoverService
 *   validates that the reason is non-blank.
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/handovers")
@RequiredArgsConstructor
@Tag(name = "Handover Requests", description = "Quản lý quy trình bàn giao tài sản (HL-01, HL-03)")
@SecurityRequirement(name = "bearerAuth")
public class HandoverController {

    private final HandoverService handoverService;

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/handovers — List all handover requests (paginated)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of all handover requests in the system.
     *
     * Accessible by SYSTEM_ADMIN, ASSET_MANAGER, and APPROVING_AUTH as defined
     * in SecurityConfig — the @PreAuthorize here is explicit for documentation clarity.
     *
     * Default sort: newest first (createdAt DESC).
     *
     * @param page Page number (0-indexed, default 0).
     * @param size Page size (default 20, max 100 recommended).
     * @return 200 OK with paginated list of HandoverDto objects.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')")
    @Operation(summary = "Danh sách yêu cầu bàn giao",
               description = "Trả về danh sách phân trang tất cả yêu cầu bàn giao. " +
                             "Mặc định sắp xếp theo thời gian tạo mới nhất trước.")
    public ResponseEntity<ApiResponse<PageResponse<HandoverDto>>> getAllHandovers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Build pageable with default sort: newest first
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HandoverDto> result = handoverService.getAllHandovers(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Tải danh sách yêu cầu bàn giao thành công",
                PageResponse.of(result)
        ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/handovers/{id} — Get one handover request by ID
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns the full detail of a single handover request.
     *
     * Includes all workflow step details (who approved, who confirmed, timestamps,
     * rejection reason if any, document reference, etc.).
     *
     * @param id UUID of the handover request.
     * @return 200 OK with the HandoverDto, or 404 if not found.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')")
    @Operation(summary = "Chi tiết yêu cầu bàn giao",
               description = "Trả về toàn bộ thông tin của một yêu cầu bàn giao theo ID.")
    public ResponseEntity<ApiResponse<HandoverDto>> getHandoverById(
            @PathVariable @Parameter(description = "UUID của yêu cầu bàn giao") UUID id) {

        HandoverDto dto = handoverService.getHandoverById(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Tải thông tin yêu cầu bàn giao thành công", dto));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/handovers — Create a new handover request (DRAFT)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new asset handover request in DRAFT status.
     *
     * The initiatedBy field is taken from the JWT token (Authentication.getName()),
     * NOT from the request body — this prevents any user from spoofing the creator.
     *
     * @param request        Validated request body (see CreateHandoverRequest).
     * @param authentication Injected by Spring Security; provides the current username.
     * @return 201 Created with the new HandoverDto.
     *         400 Bad Request if validation fails (handled by GlobalExceptionHandler).
     *         400 Bad Request if business rules are violated (e.g. duplicate active request).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Tạo yêu cầu bàn giao mới",
               description = "Tạo một yêu cầu bàn giao tài sản ở trạng thái DRAFT. " +
                             "Người tạo được xác định tự động từ token JWT.")
    public ResponseEntity<ApiResponse<HandoverDto>> createHandover(
            @Valid @RequestBody CreateHandoverRequest request,
            Authentication authentication) {

        // Extract username from JWT — NEVER trust the request body for this
        String initiatedBy = authentication.getName();
        HandoverDto created = handoverService.createHandover(request, initiatedBy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo yêu cầu bàn giao thành công", created));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/handovers/{id}/submit — DRAFT → PENDING_APPROVAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Submits a DRAFT handover request for approval.
     * Transitions the status from DRAFT to PENDING_APPROVAL.
     *
     * Typically called by the same user who created the request (or an admin).
     *
     * @param id             UUID of the handover request to submit.
     * @param authentication Current authenticated user (the submitter).
     * @return 200 OK with the updated HandoverDto in PENDING_APPROVAL status.
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Nộp yêu cầu bàn giao để phê duyệt",
               description = "Chuyển trạng thái từ DRAFT sang PENDING_APPROVAL.")
    public ResponseEntity<ApiResponse<HandoverDto>> submitHandover(
            @PathVariable UUID id,
            Authentication authentication) {

        HandoverDto updated = handoverService.submitHandover(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã nộp yêu cầu bàn giao để phê duyệt", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/handovers/{id}/approve — PENDING_APPROVAL → APPROVED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Approves a handover request (Step 1 of the approval chain).
     * Transitions: PENDING_APPROVAL → APPROVED.
     *
     * SIDE EFFECT: Calls FixedAssetService to mark the asset as TRANSFERRED and
     * update its managing unit. This cross-module call is part of HL-01.
     *
     * SEPARATION OF DUTIES: HandoverService enforces that the approver cannot be
     * the same person who initiated the request.
     *
     * @param id             UUID of the handover request to approve.
     * @param notes          Optional approval notes from the approver (query param).
     * @param authentication Current authenticated user (the approver — must be APPROVING_AUTH).
     * @return 200 OK with the updated HandoverDto in APPROVED status.
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Phê duyệt yêu cầu bàn giao",
               description = "Phê duyệt yêu cầu (bước 1). Tài sản sẽ được đánh dấu TRANSFERRED. " +
                             "Người phê duyệt không được là người tạo yêu cầu (tách biệt nhiệm vụ).")
    public ResponseEntity<ApiResponse<HandoverDto>> approveHandover(
            @PathVariable UUID id,
            @RequestParam(required = false) @Parameter(description = "Ghi chú của người phê duyệt")
            String notes,
            Authentication authentication) {

        HandoverDto updated = handoverService.approveHandover(id, notes, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Phê duyệt yêu cầu bàn giao thành công", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/handovers/{id}/confirm — APPROVED → CONFIRMED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records that the receiving unit has confirmed physical receipt of the asset.
     * Transitions: APPROVED → CONFIRMED.
     *
     * This step represents the receiving-unit representative signing off that
     * the asset physically arrived in the described condition.
     *
     * @param id             UUID of the handover request.
     * @param notes          Optional confirmation notes from the receiving unit.
     * @param authentication Current authenticated user (the confirmer).
     * @return 200 OK with the updated HandoverDto in CONFIRMED status.
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')")
    @Operation(summary = "Đơn vị tiếp nhận xác nhận bàn giao",
               description = "Đơn vị tiếp nhận xác nhận đã nhận tài sản (bước 2). " +
                             "Chuyển trạng thái từ APPROVED sang CONFIRMED.")
    public ResponseEntity<ApiResponse<HandoverDto>> confirmHandover(
            @PathVariable UUID id,
            @RequestParam(required = false) @Parameter(description = "Ghi chú xác nhận")
            String notes,
            Authentication authentication) {

        HandoverDto updated = handoverService.confirmHandover(id, notes, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận bàn giao thành công", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/handovers/{id}/complete — CONFIRMED → COMPLETED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Finalizes the handover workflow after all steps are done.
     * Transitions: CONFIRMED → COMPLETED.
     *
     * SIDE EFFECT: Triggers HandoverDocumentService.generateDocument() to produce
     * the formal "Biên bản bàn giao" document (HL-03). The document reference
     * number is stored on the request record.
     *
     * After this point, the request is permanently closed — no further transitions.
     *
     * @param id             UUID of the handover request to complete.
     * @param authentication Current authenticated user (the completer).
     * @return 200 OK with the updated HandoverDto in COMPLETED status.
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Hoàn tất quy trình bàn giao",
               description = "Đóng quy trình bàn giao và tạo biên bản bàn giao (HL-03). " +
                             "Chuyển trạng thái từ CONFIRMED sang COMPLETED.")
    public ResponseEntity<ApiResponse<HandoverDto>> completeHandover(
            @PathVariable UUID id,
            Authentication authentication) {

        HandoverDto updated = handoverService.completeHandover(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Hoàn tất bàn giao thành công. Biên bản đã được tạo.", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/handovers/{id}/reject — (active state) → REJECTED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rejects a handover request at any in-progress step.
     * Can reject from: PENDING_APPROVAL or APPROVED.
     * Cannot reject from: CONFIRMED, COMPLETED, or REJECTED.
     *
     * IMPORTANT: If the request was already APPROVED (asset marked TRANSFERRED),
     * HandoverService will automatically roll back the asset status to IN_USE
     * and restore the original managing unit.
     *
     * The rejection reason is MANDATORY — an empty or blank reason causes a
     * 400 Bad Request response from HandoverService.
     *
     * @param id             UUID of the handover request to reject.
     * @param reason         Mandatory text explaining the rejection.
     * @param authentication Current authenticated user (the rejector).
     * @return 200 OK with the updated HandoverDto in REJECTED status.
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Từ chối yêu cầu bàn giao",
               description = "Từ chối yêu cầu ở bất kỳ bước nào còn đang xử lý. " +
                             "Nếu đã phê duyệt, tài sản sẽ được hoàn trả trạng thái IN_USE. " +
                             "Lý do từ chối là bắt buộc.")
    public ResponseEntity<ApiResponse<HandoverDto>> rejectHandover(
            @PathVariable UUID id,
            @RequestParam @Parameter(description = "Lý do từ chối (bắt buộc)")
            String reason,
            Authentication authentication) {

        HandoverDto updated = handoverService.rejectHandover(id, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã từ chối yêu cầu bàn giao", updated));
    }
}