package vn.edu.hust.soict.soe.assetmanagement.handover.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ==============================================================================
 * REPOSITORY: HandoverRepository
 * ==============================================================================
 * PURPOSE:
 *   Provides all database access methods for the `handover_requests` table.
 *   Extends JpaRepository which already gives us save(), findById(), findAll(),
 *   delete(), etc. for free. This interface only adds project-specific queries.
 *
 * DESIGN DECISIONS:
 *   1. Uses JPQL (Java Persistence Query Language) rather than raw SQL so that
 *      queries remain database-agnostic and benefit from Hibernate validation.
 *   2. The "blocking" query (hasActiveRequestForAsset) is critical for enforcing
 *      the business rule: only one active request per asset at a time.
 *   3. Pagination (Page<T> + Pageable) is used for list queries to prevent
 *      memory issues with large datasets — never use findAll() for lists returned
 *      to the frontend.
 *
 * WHAT "ACTIVE" MEANS:
 *   A request is "active" (blocks new submissions) if its status is NOT one of
 *   the two terminal states: COMPLETED or REJECTED. In other words:
 *   DRAFT, PENDING_APPROVAL, APPROVED, or CONFIRMED all block a new request
 *   for the same asset.
 *
 * JPQL NOTE:
 *   In JPQL, entity and field names are the Java class/field names (camelCase),
 *   NOT the SQL table/column names. For example, "h.assetId" maps to the
 *   @Column(name = "asset_id") field.
 * ==============================================================================
 */
@Repository
public interface HandoverRepository extends JpaRepository<HandoverRequest, UUID> {

    // ── LOOKUP BY BUSINESS KEY ────────────────────────────────────────────

    /**
     * Finds a handover request by its human-readable business code (e.g. "BG-2025-001").
     * Used by HandoverService to check for duplicate codes before saving.
     *
     * @param requestCode The unique business code.
     * @return Optional containing the request if found, empty otherwise.
     */
    Optional<HandoverRequest> findByRequestCode(String requestCode);

    /**
     * Checks whether a given request code already exists in the database.
     * More efficient than findByRequestCode() when we only need a boolean answer.
     *
     * @param requestCode The code to check.
     * @return true if a record with this code exists.
     */
    boolean existsByRequestCode(String requestCode);

    // ── BLOCKING RULE: ONE ACTIVE REQUEST PER ASSET ───────────────────────

    /**
     * KEY BUSINESS RULE QUERY (HL-01):
     * Returns true if the given asset already has an active (in-progress) handover
     * request. This blocks a new submission from being created.
     *
     * "Active" = any status that is NOT COMPLETED and NOT REJECTED.
     * In other words: DRAFT, PENDING_APPROVAL, APPROVED, or CONFIRMED.
     *
     * How it works:
     *   We check for existence of any row WHERE:
     *     - assetId matches the given asset
     *     - status is NOT in the two terminal states
     *
     * @param assetId        The UUID of the asset to check.
     * @param terminalStates The two terminal statuses (COMPLETED, REJECTED) — passed
     *                       as a list so the query can exclude them.
     * @return true if a blocking active request exists.
     */
    @Query("SELECT COUNT(h) > 0 FROM HandoverRequest h " +
           "WHERE h.assetId = :assetId " +
           "AND h.status NOT IN :terminalStates")
    boolean hasActiveRequestForAsset(
            @Param("assetId") UUID assetId,
            @Param("terminalStates") List<HandoverStatus> terminalStates);

    // ── FILTERED LIST QUERIES (for the GET /api/handovers endpoint) ────────

    /**
     * Paginated list of ALL handover requests, sorted by the pageable parameter.
     * This is the primary query behind GET /api/handovers.
     *
     * The default JpaRepository.findAll(Pageable) already handles this, so this
     * method is provided via inheritance — no custom query needed.
     * (Documented here for clarity, not redeclared.)
     */

    /**
     * Returns all requests for a specific asset.
     * Useful for displaying the full handover history of one asset on its detail page.
     * Results are ordered newest-first.
     *
     * @param assetId The asset UUID to filter by.
     * @return List of all handover requests involving this asset, newest first.
     */
    List<HandoverRequest> findByAssetIdOrderByCreatedAtDesc(UUID assetId);

    /**
     * Returns all requests for a specific managing unit (either as sender or receiver).
     * Used for unit-scoped views — e.g. "show me all handovers involving my department."
     *
     * @param unitId   The managing unit UUID.
     * @param pageable Pagination and sorting parameters.
     * @return A paginated list of requests where the unit is sender or receiver.
     */
    @Query("SELECT h FROM HandoverRequest h " +
           "WHERE h.fromUnitId = :unitId OR h.toUnitId = :unitId " +
           "ORDER BY h.createdAt DESC")
    Page<HandoverRequest> findByUnitId(
            @Param("unitId") UUID unitId,
            Pageable pageable);

    /**
     * Returns all requests with a specific workflow status.
     * Useful for dashboard views like "show all PENDING_APPROVAL requests."
     *
     * @param status   The status to filter by.
     * @param pageable Pagination and sorting parameters.
     * @return A paginated list of requests in the given status.
     */
    Page<HandoverRequest> findByStatus(HandoverStatus status, Pageable pageable);

    /**
     * Returns all requests initiated by a specific user.
     * Used for "My Requests" personal dashboard views.
     *
     * @param initiatedBy The username to filter by.
     * @param pageable    Pagination and sorting parameters.
     * @return A paginated list of requests created by this user.
     */
    Page<HandoverRequest> findByInitiatedBy(String initiatedBy, Pageable pageable);
}