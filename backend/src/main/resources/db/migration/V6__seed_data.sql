-- ============================================================
-- V6__seed_data.sql
-- Description: Sample data for development and testing
-- ============================================================

-- ============================================================
-- Managing units
-- ============================================================
INSERT INTO managing_units (id, code, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'HQ',    'Ban Giám đốc',                 'Head office'),
    ('00000000-0000-0000-0000-000000000002', 'PHKT',  'Phòng Hành chính - Kỹ thuật',  'Administration & Technical dept'),
    ('00000000-0000-0000-0000-000000000003', 'PKTCN', 'Phòng Kế toán - Tài chính',    'Finance & Accounting dept'),
    ('00000000-0000-0000-0000-000000000004', 'PKD',   'Phòng Kinh doanh',             'Business dept'),
    ('00000000-0000-0000-0000-000000000005', 'PKHO',  'Phòng Kho',                    'Warehouse / Storage dept');

-- ============================================================
-- Users — one per role for testing
-- Password for ALL seed users: Password@123
-- BCrypt hash (strength 10)
-- ============================================================
INSERT INTO users (id, username, password_hash, full_name, email, is_active) VALUES

    -- R-01: System Administrator
    ('10000000-0000-0000-0000-000000000001',
     'admin',
     '$2a$10$El5iUoHNxSoRSXAxsATEX.i4roir7kBGqo9o1aiTq19oWiCCUpqy2',
     'Lê Việt Cường',
     'admin@soe.vn',
     TRUE),

    -- R-02: Asset Manager
    ('10000000-0000-0000-0000-000000000002',
     'asset.manager',
     '$2a$10$El5iUoHNxSoRSXAxsATEX.i4roir7kBGqo9o1aiTq19oWiCCUpqy2',
     'Đinh Hà Hải',
     'asset.manager@soe.vn',
     TRUE),

    -- R-03: Warehouse / Stock Officer
    ('10000000-0000-0000-0000-000000000003',
     'warehouse',
     '$2a$10$El5iUoHNxSoRSXAxsATEX.i4roir7kBGqo9o1aiTq19oWiCCUpqy2',
     'Hoàng Quốc Huy',
     'warehouse@soe.vn',
     TRUE),

    -- R-04: Approving Authority
    ('10000000-0000-0000-0000-000000000004',
     'approver',
     '$2a$10$El5iUoHNxSoRSXAxsATEX.i4roir7kBGqo9o1aiTq19oWiCCUpqy2',
     'Nguyễn Ngọc Linh',
     'approver@soe.vn',
     TRUE), 

    -- R-05: Finance & Audit Officer
    ('10000000-0000-0000-0000-000000000005',
     'finance.audit',
     '$2a$10$El5iUoHNxSoRSXAxsATEX.i4roir7kBGqo9o1aiTq19oWiCCUpqy2',
     'Trần Trung Hiếu',
     'finance.audit@soe.vn',
     TRUE);

-- ============================================================
-- Assign roles to users
-- ============================================================
INSERT INTO user_roles (user_id, role_id)
SELECT '10000000-0000-0000-0000-000000000001'::uuid, id FROM roles WHERE code = 'SYSTEM_ADMIN'
UNION ALL
SELECT '10000000-0000-0000-0000-000000000002'::uuid, id FROM roles WHERE code = 'ASSET_MANAGER'
UNION ALL
SELECT '10000000-0000-0000-0000-000000000003'::uuid, id FROM roles WHERE code = 'WAREHOUSE'
UNION ALL
SELECT '10000000-0000-0000-0000-000000000004'::uuid, id FROM roles WHERE code = 'APPROVING_AUTH'
UNION ALL
SELECT '10000000-0000-0000-0000-000000000005'::uuid, id FROM roles WHERE code = 'FINANCE_AUDIT';

-- ============================================================
-- Assign users to managing units
-- ============================================================
INSERT INTO user_units (user_id, unit_id) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'), -- admin      => HQ
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'), -- asset mgr  => PHKT
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000005'), -- warehouse  => PKHO
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001'), -- approver   => HQ
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003'); -- finance    => PKTCN

-- ============================================================
-- Storage locations
-- ============================================================
INSERT INTO storage_locations (id, code, name, unit_id) VALUES
    ('20000000-0000-0000-0000-000000000001', 'KHO-01', 'Kho Tổng',      '00000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000002', 'KHO-02', 'Kho Văn phòng', '00000000-0000-0000-0000-000000000005');

-- ============================================================
-- Sample fixed asset (for FA module testing)
-- ============================================================
INSERT INTO assets (
    id, asset_code, name, category_id, managing_unit_id,
    serial_number, manufacturer, original_cost, acquisition_date,
    funding_source, useful_life_years, salvage_value,
    depreciation_method, net_book_value, status, created_by
) VALUES (
    '30000000-0000-0000-0000-000000000001',
    'TS-2024-001',
    'Máy tính xách tay Dell Latitude 5540',
    (SELECT id FROM asset_categories WHERE code = 'IT'),
    '00000000-0000-0000-0000-000000000002',
    'SN-DELL-2024-001',
    'Dell',
    25000000.00,
    '2024-01-15',
    'Ngân sách nhà nước',
    5,
    0.00,
    'STRAIGHT_LINE',
    25000000.00,
    'IN_USE',
    'admin'
);

-- ============================================================
-- Sample material (for CS module testing)
-- ============================================================
INSERT INTO materials (
    id, material_code, name, category_id,
    unit_of_measure, unit_price, minimum_stock, created_by
) VALUES (
    '40000000-0000-0000-0000-000000000001',
    'VT-2024-001',
    'Giấy A4 IK Premium 70gsm',
    (SELECT id FROM material_categories WHERE code = 'OFFICE'),
    'Ream',
    85000.00,
    10.000,
    'admin'
);

-- ============================================================
-- Test credentials summary
-- All passwords: Password@123
-- ============================================================
-- admin          -> R-01 SYSTEM_ADMIN    -> user/config management, full audit access
-- asset.manager  -> R-02 ASSET_MANAGER   -> create/manage assets, initiate handovers
-- warehouse      -> R-03 WAREHOUSE       -> stock catalogue, receipts, issues
-- approver       -> R-04 APPROVING_AUTH  -> approve stock, handovers, liquidations
-- finance.audit  -> R-05 FINANCE_AUDIT   -> read-only, reports, audit trail
-- ============================================================