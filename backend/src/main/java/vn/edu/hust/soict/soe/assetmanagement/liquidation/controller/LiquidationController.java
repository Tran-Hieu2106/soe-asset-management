package vn.edu.hust.soict.soe.assetmanagement.liquidation.controller;

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
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.CreateLiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.dto.LiquidationDto;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.service.LiquidationService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ==============================================================================
 * CONTROLLER: LiquidationController
 * ==============================================================================
 * PURPOSE:
 *   Exposes the REST API for the asset liquidation workflow (HL-02, HL-02a, HL-03).
 *   This controller is a thin HTTP adapter — it handles request parsing,
 *   authentication extraction, and response wrapping. ALL business logic
 *   lives in LiquidationService.
 *
 * ARCHITECTURE RULES FOLLOWED:
 *   1. Every response is wrapped in ApiResponse<T>          (project standard)
 *   2. List responses are additionally wrapped in PageResponse<T>
 *   3. Username is ALWAYS read from authentication.getName() (the JWT token),
 *      NEVER from the request body — prevents identity spoofing attacks
 *   4. No try/catch blocks — GlobalExceptionHandler handles all exceptions
 *      and maps them to the correct HTTP status codes
 *   5. No business logic — all validation and processing is in LiquidationService
 *
 * SECURITY (from SecurityConfig.java):
 *   /api/liquidations/** → hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')
 *   Additional @PreAuthorize on specific methods:
 *     - Creating and submitting  → ASSET_MANAGER initiates disposal requests
 *     - Manager-level approval   → APPROVING_AUTH acts as the manager approver
 *     - Director-level approval  → APPROVING_AUTH also acts as director approver
 *       (In a real deployment this might be a separate DIRECTOR role, but per
 *       the SRS role table only 5 roles are defined, and APPROVING_AUTH covers both)
 *     - Completing               → SYSTEM_ADMIN or ASSET_MANAGER finalises the record
 *     - Rejecting                → APPROVING_AUTH at either step
 *     - Reading                  → all three roles
 *
 * API ENDPOINTS:
 *   GET  /api/liquidations                           → list all (paginated)
 *   GET  /api/liquidations/{id}                      → get one by ID
 *   POST /api/liquidations                           → create new (DRAFT)
 *   PUT  /api/liquidations/{id}/submit               → DRAFT → PENDING_MANAGER
 *   PUT  /api/liquidations/{id}/approve-manager      → PENDING_MANAGER → PENDING_DIRECTOR
 *   PUT  /api/liquidations/{id}/approve-director     → PENDING_DIRECTOR → APPROVED
 *   PUT  /api/liquidations/{id}/complete             → APPROVED → COMPLETED (asset LIQUIDATED)
 *   PUT  /api/liquidations/{id}/reject               → PENDING_MANAGER or PENDING_DIRECTOR → REJECTED
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/liquidations")
@RequiredArgsConstructor
@Tag(name = "Liquidation Requests",
     description = "Quản lý quy trình thanh lý tài sản (HL-02, HL-02a, HL-03)")
@SecurityRequirement(name = "bearerAuth")
public class LiquidationController {

    private final LiquidationService liquidationService;

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/liquidations — Paginated list of all liquidation requests
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of all liquidation requests.
     * Default sort: newest first (createdAt DESC).
     *
     * @param page 0-indexed page number (default 0).
     * @param size Number of items per page (default 20).
     * @return 200 OK with ApiResponse wrapping PageResponse<LiquidationDto>.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')")
    @Operation(summary = "Danh sách yêu cầu thanh lý",
               description = "Trả về danh sách phân trang tất cả yêu cầu thanh lý tài sản. " +
                             "Mặc định sắp xếp theo thời gian tạo mới nhất trước.")
    public ResponseEntity<ApiResponse<PageResponse<LiquidationDto>>> getAllLiquidations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Build Pageable with newest-first sort to match the handover module pattern
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<LiquidationDto> result = liquidationService.getAllLiquidations(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Tải danh sách yêu cầu thanh lý thành công",
                PageResponse.of(result)
        ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/liquidations/{id} — Full detail of one request
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns the complete detail of a single liquidation request, including
     * all step timestamps, approval notes, and document reference.
     *
     * @param id UUID of the liquidation request.
     * @return 200 OK with LiquidationDto, or 404 if not found.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER', 'APPROVING_AUTH')")
    @Operation(summary = "Chi tiết yêu cầu thanh lý",
               description = "Trả về toàn bộ thông tin của một yêu cầu thanh lý theo ID.")
    public ResponseEntity<ApiResponse<LiquidationDto>> getLiquidationById(
            @PathVariable @Parameter(description = "UUID của yêu cầu thanh lý") UUID id) {

        LiquidationDto dto = liquidationService.getLiquidationById(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Tải thông tin yêu cầu thanh lý thành công", dto));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/liquidations — Create a new DRAFT liquidation request
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new liquidation request in DRAFT status.
     *
     * The initiatedBy is always extracted from the JWT token, never the request body.
     * Bean validation (@Valid) runs before this method body executes — any
     * constraint violation returns 400 before the service is even called.
     *
     * @param request        Validated DTO from the POST body.
     * @param authentication Spring Security Authentication from the JWT filter.
     * @return 201 Created with the new LiquidationDto.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Tạo yêu cầu thanh lý mới",
               description = "Tạo một yêu cầu thanh lý tài sản ở trạng thái DRAFT. " +
                             "Người tạo được xác định tự động từ token JWT.")
    public ResponseEntity<ApiResponse<LiquidationDto>> createLiquidation(
            @Valid @RequestBody CreateLiquidationRequest request,
            Authentication authentication) {

        // Always extract the username from the JWT — never from the request body
        String initiatedBy = authentication.getName();
        LiquidationDto created = liquidationService.createLiquidation(request, initiatedBy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo yêu cầu thanh lý thành công", created));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/liquidations/{id}/submit — DRAFT → PENDING_MANAGER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Submits a DRAFT request to the manager approval queue.
     * Typically called by the same person who created the draft (or an admin).
     *
     * @param id             UUID of the request to submit.
     * @param authentication JWT-authenticated user (the submitter).
     * @return 200 OK with updated LiquidationDto in PENDING_MANAGER status.
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Nộp yêu cầu thanh lý để xét duyệt",
               description = "Chuyển trạng thái từ DRAFT sang PENDING_MANAGER.")
    public ResponseEntity<ApiResponse<LiquidationDto>> submitLiquidation(
            @PathVariable UUID id,
            Authentication authentication) {

        LiquidationDto updated = liquidationService.submitLiquidation(
                id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã nộp yêu cầu thanh lý để xét duyệt cấp quản lý", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/liquidations/{id}/approve-manager — PENDING_MANAGER → PENDING_DIRECTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records Step 1 (manager-level) approval.
     * Transitions: PENDING_MANAGER → PENDING_DIRECTOR.
     *
     * SEPARATION OF DUTIES (BR-02): The approver must not be the request initiator.
     * This is enforced in LiquidationService — it cannot be bypassed by any role.
     *
     * @param id             UUID of the request.
     * @param notes          Optional approval notes (query param).
     * @param authentication JWT user performing the approval.
     * @return 200 OK with updated LiquidationDto in PENDING_DIRECTOR status.
     */
    @PutMapping("/{id}/approve-manager")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Phê duyệt cấp quản lý",
               description = "Bước 1: Phê duyệt cấp quản lý. Người phê duyệt không được là " +
                             "người tạo yêu cầu (tách biệt nhiệm vụ). " +
                             "Chuyển trạng thái từ PENDING_MANAGER sang PENDING_DIRECTOR.")
    public ResponseEntity<ApiResponse<LiquidationDto>> approveManagerStep(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @Parameter(description = "Ghi chú của người phê duyệt cấp quản lý")
            String notes,
            Authentication authentication) {

        LiquidationDto updated = liquidationService.approveManagerStep(
                id, notes, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Phê duyệt cấp quản lý thành công", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/liquidations/{id}/approve-director — PENDING_DIRECTOR → APPROVED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Records Step 2 (director-level) approval.
     * Transitions: PENDING_DIRECTOR → APPROVED.
     *
     * After this step the request is fully authorised. The final COMPLETED
     * transition (which marks the asset LIQUIDATED) is a separate step.
     *
     * @param id             UUID of the request.
     * @param notes          Optional director notes (query param).
     * @param authentication JWT user performing the director-level approval.
     * @return 200 OK with updated LiquidationDto in APPROVED status.
     */
    @PutMapping("/{id}/approve-director")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Phê duyệt cấp giám đốc",
               description = "Bước 2: Phê duyệt cấp giám đốc/hội đồng. " +
                             "Chuyển trạng thái từ PENDING_DIRECTOR sang APPROVED.")
    public ResponseEntity<ApiResponse<LiquidationDto>> approveDirectorStep(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @Parameter(description = "Ghi chú của người phê duyệt cấp giám đốc")
            String notes,
            Authentication authentication) {

        LiquidationDto updated = liquidationService.approveDirectorStep(
                id, notes, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Phê duyệt cấp giám đốc thành công", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/liquidations/{id}/complete — APPROVED → COMPLETED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Finalises the liquidation workflow and permanently closes the asset.
     * Transitions: APPROVED → COMPLETED.
     *
     * CRITICAL SIDE EFFECTS (HL-02a):
     *   - The asset status is permanently set to LIQUIDATED
     *   - A LIQUIDATED event is appended to asset_history
     *   - The Biên bản thanh lý document reference is generated (HL-03)
     *   - Audit log entries are written
     *   All of the above happen atomically in one @Transactional context.
     *
     * After this call, the asset is PERMANENTLY READ-ONLY (BR-05).
     * No further modifications to that asset's profile are permitted.
     *
     * @param id                 UUID of the request to complete.
     * @param finalDisposalValue Actual money received from disposal (VND). Optional.
     * @param authentication     JWT user finalising the record.
     * @return 200 OK with updated LiquidationDto in COMPLETED status.
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ASSET_MANAGER')")
    @Operation(summary = "Hoàn tất quy trình thanh lý",
               description = "Đóng quy trình thanh lý. Tài sản sẽ bị đánh dấu LIQUIDATED " +
                             "vĩnh viễn và không thể sửa đổi (BR-05). " +
                             "Tạo biên bản thanh lý (HL-03). " +
                             "Chuyển trạng thái từ APPROVED sang COMPLETED.")
    public ResponseEntity<ApiResponse<LiquidationDto>> completeLiquidation(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @Parameter(description = "Giá trị thực tế thu được từ thanh lý (VNĐ)")
            BigDecimal finalDisposalValue,
            Authentication authentication) {

        LiquidationDto updated = liquidationService.completeLiquidation(
                id, finalDisposalValue, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Hoàn tất thanh lý thành công. Biên bản đã được tạo. " +
                "Tài sản đã bị thanh lý.", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/liquidations/{id}/reject — PENDING_MANAGER or PENDING_DIRECTOR → REJECTED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rejects a liquidation request at Step 1 or Step 2 of the approval chain.
     *
     * Can reject from: PENDING_MANAGER or PENDING_DIRECTOR.
     * Cannot reject from: DRAFT, APPROVED, COMPLETED, or REJECTED.
     *
     * IMPORTANT: Unlike the handover module, there is NO asset rollback on rejection
     * because the asset was never modified during the liquidation workflow.
     * The asset remains in whatever state it was in before the request was created.
     *
     * The rejection reason is MANDATORY. An empty reason causes a 400 Bad Request.
     *
     * @param id             UUID of the request to reject.
     * @param reason         Mandatory Vietnamese explanation of the rejection.
     * @param authentication JWT user performing the rejection.
     * @return 200 OK with updated LiquidationDto in REJECTED status.
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'APPROVING_AUTH')")
    @Operation(summary = "Từ chối yêu cầu thanh lý",
               description = "Từ chối yêu cầu tại bước 1 (PENDING_MANAGER) hoặc bước 2 " +
                             "(PENDING_DIRECTOR). Không thể từ chối khi đã APPROVED. " +
                             "Không cần hoàn trả trạng thái tài sản (tài sản chưa bị thay đổi). " +
                             "Lý do từ chối là bắt buộc.")
    public ResponseEntity<ApiResponse<LiquidationDto>> rejectLiquidation(
            @PathVariable UUID id,
            @RequestParam @Parameter(description = "Lý do từ chối (bắt buộc)")
            String reason,
            Authentication authentication) {

        LiquidationDto updated = liquidationService.rejectLiquidation(
                id, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã từ chối yêu cầu thanh lý", updated));
    }
}