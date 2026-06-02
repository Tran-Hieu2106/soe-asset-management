-- ============================================================
-- V3__create_stock.sql
-- Requirements: CS-01, CS-02, CS-03, CS-04
-- ============================================================

-- Material categories
CREATE TABLE material_categories (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT
);

-- Seed common material categories
INSERT INTO material_categories (code, name) VALUES
    ('OFFICE',      'Văn phòng phẩm'),
    ('EQUIPMENT',   'Thiết bị điện tử'),
    ('CHEMICAL',    'Hóa chất'),
    ('FUEL',        'Nhiên liệu'),
    ('SPARE',       'Phụ tùng thay thế'),
    ('OTHER',       'Vật tư khác');

-- Material catalogue CS-01
CREATE TABLE materials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_code       VARCHAR(50)     NOT NULL UNIQUE,   -- e.g. VT-2024-001
    name                VARCHAR(255)    NOT NULL,
    category_id         INTEGER         NOT NULL REFERENCES material_categories(id),
    unit_of_measure     VARCHAR(30)     NOT NULL,          -- e.g. Cái, Kg, Lít, Hộp, Cuộn
    technical_specs     TEXT,
    supplier_name       VARCHAR(255),
    supplier_code       VARCHAR(100),
    unit_price          NUMERIC(18, 2),                    -- reference unit price
    minimum_stock       NUMERIC(12, 3)  NOT NULL DEFAULT 0, -- reorder threshold
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    notes               TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100)    NOT NULL
);

-- Storage locations (warehouses)
CREATE TABLE storage_locations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    unit_id     UUID         NOT NULL REFERENCES managing_units(id),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100)
);

-- Stock transactions — CS-02 (every receipt and issue)
-- CS-03: current balance = SUM(qty) WHERE type=RECEIPT minus SUM(qty) WHERE type=ISSUE
-- Do NOT store balance as a column — compute it from this table
CREATE TABLE stock_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id             UUID            NOT NULL REFERENCES materials(id),
    storage_location_id     UUID            NOT NULL REFERENCES storage_locations(id),

    transaction_type        VARCHAR(10)     NOT NULL,
    -- RECEIPT (stock in) | ISSUE (stock out)

    quantity                NUMERIC(12, 3)  NOT NULL CHECK (quantity > 0),
    unit_of_measure         VARCHAR(30)     NOT NULL,   -- copied from material at time of transaction
    unit_price              NUMERIC(18, 2),             -- price at time of transaction
    total_value             NUMERIC(18, 2),             -- = quantity * unit_price

    -- CS-04: departmental allocation (required for ISSUE transactions)
    requesting_department_id UUID           REFERENCES managing_units(id),
    requested_by            VARCHAR(100),               -- name of staff who requested

    -- Reference documents
    document_ref            VARCHAR(255)    NOT NULL,   -- receipt/issue slip number
    document_date           DATE            NOT NULL,
    notes                   TEXT,

    -- Approval
    approved_by             VARCHAR(100),
    approved_at             TIMESTAMP,

    -- Audit fields
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100)    NOT NULL
    -- NO updated_at — transactions are immutable once created
);

-- Indexes
CREATE INDEX idx_materials_code         ON materials(material_code);
CREATE INDEX idx_materials_category     ON materials(category_id);
CREATE INDEX idx_stock_tx_material      ON stock_transactions(material_id);
CREATE INDEX idx_stock_tx_location      ON stock_transactions(storage_location_id);
CREATE INDEX idx_stock_tx_type          ON stock_transactions(transaction_type);
CREATE INDEX idx_stock_tx_dept          ON stock_transactions(requesting_department_id);
CREATE INDEX idx_stock_tx_date          ON stock_transactions(document_date);

-- Note for Huy:
-- CS-03 real-time balance query:
--   SELECT
--     material_id,
--     storage_location_id,
--     SUM(CASE WHEN transaction_type = 'RECEIPT' THEN quantity ELSE -quantity END) AS current_balance
--   FROM stock_transactions
--   GROUP BY material_id, storage_location_id;
--
-- CS-04 departmental usage query:
--   SELECT
--     requesting_department_id,
--     material_id,
--     SUM(quantity) AS total_issued,
--     SUM(total_value) AS total_value
--   FROM stock_transactions
--   WHERE transaction_type = 'ISSUE'
--     AND document_date BETWEEN :start AND :end
--   GROUP BY requesting_department_id, material_id;