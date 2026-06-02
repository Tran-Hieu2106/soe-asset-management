package vn.edu.hust.soict.soe.assetmanagement.liquidation.entity;

/**
 * ==============================================================================
 * ENUM: LiquidationStatus
 * ==============================================================================
 * PURPOSE:
 *   Defines every legal state a LiquidationRequest can occupy during its lifecycle.
 *   This enum is stored as a VARCHAR(30) string in the `liquidation_requests` table
 *   (column: status) via @Enumerated(EnumType.STRING) on the entity.
 *
 * WORKFLOW (two-step approval, one terminal failure state):
 *
 *   DRAFT
 *     │  (initiator submits the request)
 *     ▼
 *   PENDING_MANAGER
 *     │  (Asset Manager reviews and approves — Step 1)
 *     ├──[reject]──► REJECTED  (terminal)
 *     ▼
 *   PENDING_DIRECTOR
 *     │  (Director / board reviews and approves — Step 2)
 *     ├──[reject]──► REJECTED  (terminal)
 *     ▼
 *   APPROVED
 *     │  (System/admin finalises record: asset set to LIQUIDATED, document generated)
 *     ▼
 *   COMPLETED  (terminal — asset permanently read-only, workflow closed)
 *
 * DB CONTRACT (V4__create_handover_liquidation.sql):
 *   Column `status VARCHAR(30)` uses the exact string names below.
 *   Do NOT rename enum constants without a Flyway migration that updates
 *   any existing rows containing the old name.
 *
 * KEY DIFFERENCE FROM HANDOVER:
 *   Liquidation has TWO approval steps (manager + director) before the request
 *   is APPROVED, whereas handover has only ONE. This reflects the higher
 *   organisational authority required to permanently dispose of state assets
 *   under Nghị định 151/2017/NĐ-CP.
 *
 * INTEGRATION NOTES:
 *   - LiquidationService enforces valid transitions.
 *   - When status reaches COMPLETED, LiquidationService calls
 *     FixedAssetService.updateAssetStatus() to set the asset to LIQUIDATED.
 *     After this point the asset record is permanently read-only (BR-05).
 *   - COMPLETED and REJECTED are the only states from which no further
 *     transitions are possible.
 * ==============================================================================
 */
public enum LiquidationStatus {

    /**
     * The request has been created but not yet submitted.
     * The initiator can still edit details before submitting.
     */
    DRAFT,

    /**
     * The request has been submitted and is awaiting Step 1 approval
     * from the Asset Manager level (APPROVING_AUTH role in this system).
     * Business rule: the approver at this step must not be the initiator.
     */
    PENDING_MANAGER,

    /**
     * Step 1 (manager level) has approved. The request is now awaiting
     * Step 2 approval from the Director / board level.
     * Asset has NOT been modified yet — no changes to the asset table
     * happen until COMPLETED.
     */
    PENDING_DIRECTOR,

    /**
     * Both approval steps have been completed. The request is now awaiting
     * the final completion action that will permanently close the asset.
     * In this state the request is fully authorised for disposal.
     */
    APPROVED,

    /**
     * Terminal success state. Final completion triggered:
     *  1. Asset status set to LIQUIDATED (permanently read-only — BR-05)
     *  2. Final disposal value recorded
     *  3. LIQUIDATED event appended to asset_history (FA-04)
     *  4. Biên bản thanh lý document generated (HL-03)
     *  5. Audit log written (RP-01)
     * No further transitions from this state are possible.
     */
    COMPLETED,

    /**
     * Terminal failure state. The request was rejected at Step 1 or Step 2.
     * The rejection_reason column captures who rejected it and why.
     * Because the asset was never modified during the workflow, no rollback
     * is needed on rejection (unlike the handover module).
     * A new request CAN be submitted for the same asset after rejection.
     */
    REJECTED
}