package vn.edu.hust.soict.soe.assetmanagement.handover.entity;

/**
 * ==============================================================================
 * ENUM: HandoverStatus
 * ==============================================================================
 * PURPOSE:
 *   Defines every legal state a HandoverRequest can occupy during its lifecycle.
 *   This enum is stored as a VARCHAR(30) string in the `handover_requests` table
 *   (column: status) via @Enumerated(EnumType.STRING) on the entity.
 *
 * WORKFLOW (linear, with one terminal failure branch):
 *
 *   DRAFT
 *     │  (initiator submits the request)
 *     ▼
 *   PENDING_APPROVAL
 *     │  (department head / APPROVING_AUTH reviews)
 *     ├──[reject]──► REJECTED  (terminal — no further transitions allowed)
 *     ▼
 *   APPROVED
 *     │  (receiving unit acknowledges and confirms receipt)
 *     ├──[reject]──► REJECTED
 *     ▼
 *   CONFIRMED
 *     │  (final bookkeeping: asset record updated, document signed)
 *     ▼
 *   COMPLETED  (terminal — asset ownership transferred, workflow closed)
 *
 * DB CONTRACT (V4__create_handover_liquidation.sql):
 *   Column `status VARCHAR(30)` uses the exact string names below.
 *   Do NOT rename enum constants without a matching Flyway migration.
 *
 * INTEGRATION NOTES:
 *   - HandoverService.java enforces valid transitions (e.g., cannot go
 *     from DRAFT straight to CONFIRMED).
 *   - When status reaches APPROVED, HandoverService calls
 *     FixedAssetService.updateAssetStatusAndUnit() to mark the asset as
 *     TRANSFERRED and update its managing unit.
 *   - COMPLETED is the only state after which no further actions are allowed
 *     (along with REJECTED).
 * ==============================================================================
 */
public enum HandoverStatus {

    /**
     * The request has been created but not yet submitted for approval.
     * The initiator can still edit or delete it.
     */
    DRAFT,

    /**
     * The request has been formally submitted and is awaiting department-head
     * / APPROVING_AUTH review (Step 1 of the approval chain).
     */
    PENDING_APPROVAL,

    /**
     * The department head has approved the transfer.
     * The receiving unit must now confirm receipt (Step 2).
     * At this point, FixedAssetService.updateAssetStatusAndUnit() is called
     * to mark the asset as TRANSFERRED.
     */
    APPROVED,

    /**
     * The receiving unit has confirmed they have received the asset (Step 2).
     * Final bookkeeping (document signing, record closure) is pending.
     */
    CONFIRMED,

    /**
     * Terminal success state. All steps completed:
     *  1. Approved by department head
     *  2. Confirmed by receiving unit
     *  3. Asset record updated and document finalized
     */
    COMPLETED,

    /**
     * Terminal failure state. The request was rejected at any step in the chain.
     * The rejection_reason column records who rejected it and why.
     * A new request can be submitted for the same asset after rejection.
     */
    REJECTED
}