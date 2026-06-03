-- ============================================================
-- V5__create_audit_log.sql
-- Requirement: RP-01
-- ============================================================

-- Immutable audit log — RP-01
-- Every create, update, delete, and status-change event across
-- ALL modules writes one row here.
-- This table must NEVER have UPDATE or DELETE executed against it.
-- Enforced at application level (no update/delete methods in AuditLogRepository)
-- and recommended at DB level via the rule below.

CREATE TABLE audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- What happened
    module          VARCHAR(50) NOT NULL,
    -- ASSET | STOCK | HANDOVER | LIQUIDATION | USER | AUTH

    action          VARCHAR(50) NOT NULL,
    -- CREATE | UPDATE | DELETE | STATUS_CHANGE | LOGIN | LOGOUT |
    -- APPROVE | REJECT | CONFIRM | EXPORT

    -- Which record was affected
    record_id       VARCHAR(255),        -- UUID or code of the affected record
    record_code     VARCHAR(100),        -- human-readable code e.g. TS-2024-001

    -- Who did it
    performed_by    VARCHAR(100) NOT NULL,   -- username
    user_id         UUID         REFERENCES users(id) ON DELETE SET NULL,
    ip_address      VARCHAR(45),             -- IPv4 or IPv6

    -- What changed (RP-01: before/after values)
    old_value       TEXT,       -- JSON string of fields before the change
    new_value       TEXT,       -- JSON string of fields after the change
    description     TEXT,       -- human-readable summary e.g. "Asset TS-001 status changed to IDLE"

    -- When
    performed_at    TIMESTAMP   NOT NULL DEFAULT NOW()

    -- No created_at/updated_at/created_by — performed_at is the only timestamp needed
    -- No primary key other than id — append-only, no updates ever
);

-- Prevent any application-level UPDATE or DELETE on this table
-- (PostgreSQL rule — blocks at DB level as a safety net)
CREATE RULE no_update_audit_log AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_log AS ON DELETE TO audit_logs DO INSTEAD NOTHING;

-- Indexes
CREATE INDEX idx_audit_module       ON audit_logs(module);
CREATE INDEX idx_audit_action       ON audit_logs(action);
CREATE INDEX idx_audit_record_id    ON audit_logs(record_id);
CREATE INDEX idx_audit_user         ON audit_logs(user_id);
CREATE INDEX idx_audit_performed_at ON audit_logs(performed_at DESC);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);