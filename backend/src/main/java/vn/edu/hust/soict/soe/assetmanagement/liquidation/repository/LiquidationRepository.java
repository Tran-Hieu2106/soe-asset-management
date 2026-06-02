package vn.edu.hust.soict.soe.assetmanagement.liquidation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ==============================================================================
 * REPOSITORY: LiquidationRepository
 * ==============================================================================
 * PURPOSE:
 *   Provides all database access methods for the `liquidation_requests` table.
 *   Extends JpaRepository<LiquidationRequest, UUID> which gives save(), findById(),
 *   findAll(Pageable), count(), etc. for free. This interface only adds
 *   project-specific queries that JPA cannot auto-derive.
 *
 * DESIGN PRINCIPLES:
 *   1. JPQL (not raw SQL): queries use Java class/field names (camelCase), not SQL
 *      column names (snake_case). For example, "l.assetId" in JPQL maps to
 *      the "asset_id" column via the @Column annotation on the entity.
 *   2. Pagination first: any method that could return many rows uses Page<T> + Pageable
 *      to protect against large memory allocations on the application server.
 *   3. No business logic: this interface only declares queries. All decisions
 *      (e.g. "is this request rejectable?") live in LiquidationService.
 *
 * WHAT "ACTIVE" MEANS FOR BLOCKING RULE:
 *   A liquidation request is "active" (blocks new submissions for the same asset)
 *   if its status is NOT COMPLETED and NOT REJECTED, i.e.:
 *     DRAFT, PENDING_MANAGER, PENDING_DIRECTOR, or APPROVED.
 *   Once COMPLETED or REJECTED, a new request for the same asset is permitted.
 *
 * NOTE ON UNIQUE CODES:
 *   The `request_code` column has a UNIQUE constraint in the DB. If two threads
 *   simultaneously generate the same code (low probability at SOE scale),
 *   the DB constraint catches it and throws DataIntegrityViolationException.
 *   The count()-based generation strategy is adequate for this system.
 * ==============================================================================
 */
@Repository
public interface LiquidationRepository extends JpaRepository<LiquidationRequest, UUID> {

    // ── LOOKUP BY BUSINESS KEY ────────────────────────────────────────────

    /**
     * Finds a liquidation request by its human-readable business code (e.g. "TL-2025-0001").
     *
     * @param requestCode The unique business code.
     * @return Optional containing the request if found, empty otherwise.
     */
    Optional<LiquidationRequest> findByRequestCode(String requestCode);

    /**
     * Checks whether a request code already exists. More efficient than
     * findByRequestCode() when only a boolean answer is needed.
     *
     * @param requestCode The code to check.
     * @return true if a record with this code already exists.
     */
    boolean existsByRequestCode(String requestCode);

    // ── BLOCKING RULE: ONE ACTIVE REQUEST PER ASSET ───────────────────────

    /**
     * KEY BUSINESS RULE QUERY (HL-02):
     * Returns true if the given asset already has an active (in-progress) liquidation
     * request. This prevents a new request from being created while one is pending.
     *
     * "Active" = status is NOT in the terminal states list (COMPLETED, REJECTED).
     * i.e.: DRAFT, PENDING_MANAGER, PENDING_DIRECTOR, or APPROVED all block a new request.
     *
     * Uses COUNT(l) > 0 instead of EXISTS for broad JPA compatibility.
     *
     * @param assetId        UUID of the asset to check.
     * @param terminalStates List of statuses that do NOT block a new request
     *                       (passed as [COMPLETED, REJECTED] by the service).
     * @return true if a blocking in-progress request exists.
     */
    @Query("SELECT COUNT(l) > 0 FROM LiquidationRequest l " +
           "WHERE l.assetId = :assetId " +
           "AND l.status NOT IN :terminalStates")
    boolean hasActiveRequestForAsset(
            @Param("assetId") UUID assetId,
            @Param("terminalStates") List<LiquidationStatus> terminalStates);

    // ── FILTERED LIST QUERIES ─────────────────────────────────────────────

    /**
     * Returns all liquidation requests for a specific asset, ordered newest-first.
     * Used to display the full liquidation history on an asset's detail page.
     *
     * @param assetId The asset UUID to filter by.
     * @return List of all liquidation requests for this asset, newest first.
     */
    List<LiquidationRequest> findByAssetIdOrderByCreatedAtDesc(UUID assetId);

    /**
     * Returns all liquidation requests submitted by a specific managing unit.
     * Used for unit-scoped views: "show all liquidation requests from my department."
     *
     * @param requestingUnitId The managing unit UUID.
     * @param pageable         Pagination and sorting parameters.
     * @return Paginated list of requests from the given unit.
     */
    Page<LiquidationRequest> findByRequestingUnitId(UUID requestingUnitId, Pageable pageable);

    /**
     * Returns all requests in a specific workflow status.
     * Used for dashboard views like "show all PENDING_MANAGER requests for approval."
     *
     * @param status   The workflow status to filter by.
     * @param pageable Pagination and sorting parameters.
     * @return Paginated list of requests in the given status.
     */
    Page<LiquidationRequest> findByStatus(LiquidationStatus status, Pageable pageable);

    /**
     * Returns all requests initiated by a specific username.
     * Used for personal "My Requests" dashboard views.
     *
     * @param initiatedBy The username to filter by.
     * @param pageable    Pagination and sorting parameters.
     * @return Paginated list of requests created by this user.
     */
    Page<LiquidationRequest> findByInitiatedBy(String initiatedBy, Pageable pageable);
}