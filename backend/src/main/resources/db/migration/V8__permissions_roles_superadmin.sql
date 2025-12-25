-- V8: Permissions, Roles and Superadmin user
-- System for role-based access control (RBAC)

-- =====================================================
-- PERMISSIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ROLES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ROLES_PERMISSIONS (Many-to-Many)
-- =====================================================

CREATE TABLE IF NOT EXISTS roles_permissions (
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =====================================================
-- USER_COMPANIES TABLE (User-Company-Role relationship)
-- =====================================================

CREATE TABLE IF NOT EXISTS user_companies (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, company_id)
);

CREATE INDEX idx_user_companies_user ON user_companies(user_id);
CREATE INDEX idx_user_companies_company ON user_companies(company_id);

-- =====================================================
-- INSERT PERMISSIONS
-- =====================================================

INSERT INTO permissions (name) VALUES
    -- Company management
    ('COMPANY_CREATE'),
    ('COMPANY_EDIT'),
    ('COMPANY_DELETE'),
    ('COMPANY_VIEW'),

    -- User management
    ('USER_CREATE'),
    ('USER_EDIT'),
    ('USER_DELETE'),
    ('USER_VIEW'),
    ('USER_MANAGE_ROLES'),

    -- Journal entries
    ('JOURNAL_CREATE'),
    ('JOURNAL_EDIT'),
    ('JOURNAL_DELETE'),
    ('JOURNAL_POST'),
    ('JOURNAL_UNPOST'),
    ('JOURNAL_VIEW'),

    -- Chart of accounts
    ('ACCOUNT_CREATE'),
    ('ACCOUNT_EDIT'),
    ('ACCOUNT_DELETE'),
    ('ACCOUNT_VIEW'),

    -- Counterparts
    ('COUNTERPART_CREATE'),
    ('COUNTERPART_EDIT'),
    ('COUNTERPART_DELETE'),
    ('COUNTERPART_VIEW'),

    -- VAT
    ('VAT_RETURN_CREATE'),
    ('VAT_RETURN_EDIT'),
    ('VAT_RETURN_SUBMIT'),
    ('VAT_RETURN_VIEW'),

    -- Fixed assets
    ('FIXED_ASSET_CREATE'),
    ('FIXED_ASSET_EDIT'),
    ('FIXED_ASSET_DELETE'),
    ('FIXED_ASSET_DEPRECIATE'),
    ('FIXED_ASSET_DISPOSE'),
    ('FIXED_ASSET_VIEW'),

    -- Banking
    ('BANK_IMPORT'),
    ('BANK_PROFILE_MANAGE'),

    -- Reports
    ('REPORT_VIEW'),
    ('REPORT_EXPORT'),

    -- System
    ('SYSTEM_ADMIN'),
    ('SETTINGS_MANAGE')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- INSERT ROLES
-- =====================================================

INSERT INTO roles (name) VALUES
    ('SUPERADMIN'),
    ('ADMIN'),
    ('ACCOUNTANT'),
    ('OPERATOR'),
    ('VIEWER')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- ASSIGN PERMISSIONS TO ROLES
-- =====================================================

-- SUPERADMIN gets all permissions
INSERT INTO roles_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'SUPERADMIN'),
    id
FROM permissions
ON CONFLICT DO NOTHING;

-- ADMIN gets all permissions except SYSTEM_ADMIN
INSERT INTO roles_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    id
FROM permissions
WHERE name != 'SYSTEM_ADMIN'
ON CONFLICT DO NOTHING;

-- ACCOUNTANT permissions
INSERT INTO roles_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ACCOUNTANT'),
    id
FROM permissions
WHERE name IN (
    'JOURNAL_CREATE', 'JOURNAL_EDIT', 'JOURNAL_DELETE', 'JOURNAL_POST', 'JOURNAL_UNPOST', 'JOURNAL_VIEW',
    'ACCOUNT_VIEW',
    'COUNTERPART_CREATE', 'COUNTERPART_EDIT', 'COUNTERPART_DELETE', 'COUNTERPART_VIEW',
    'VAT_RETURN_CREATE', 'VAT_RETURN_EDIT', 'VAT_RETURN_SUBMIT', 'VAT_RETURN_VIEW',
    'FIXED_ASSET_CREATE', 'FIXED_ASSET_EDIT', 'FIXED_ASSET_DELETE', 'FIXED_ASSET_DEPRECIATE', 'FIXED_ASSET_DISPOSE', 'FIXED_ASSET_VIEW',
    'BANK_IMPORT',
    'REPORT_VIEW', 'REPORT_EXPORT',
    'COMPANY_VIEW'
)
ON CONFLICT DO NOTHING;

-- OPERATOR permissions
INSERT INTO roles_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'OPERATOR'),
    id
FROM permissions
WHERE name IN (
    'JOURNAL_CREATE', 'JOURNAL_EDIT', 'JOURNAL_VIEW',
    'COUNTERPART_CREATE', 'COUNTERPART_EDIT', 'COUNTERPART_VIEW',
    'ACCOUNT_VIEW',
    'REPORT_VIEW',
    'COMPANY_VIEW'
)
ON CONFLICT DO NOTHING;

-- VIEWER permissions
INSERT INTO roles_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'VIEWER'),
    id
FROM permissions
WHERE name IN (
    'COMPANY_VIEW',
    'ACCOUNT_VIEW',
    'COUNTERPART_VIEW',
    'JOURNAL_VIEW',
    'REPORT_VIEW',
    'VAT_RETURN_VIEW',
    'FIXED_ASSET_VIEW'
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- CREATE SUPERADMIN USER
-- IMPORTANT: Change this password immediately after first login!
-- =====================================================

INSERT INTO users (username, email, password_hash, first_name, last_name)
VALUES (
    'superadmin',
    'admin@example.com',
    '$2b$12$CzzH9KBptDSThQtWVzWvWuNXA/Cln8I4UHNn.2Y3kYlsJNqCnCoyK',
    'Super',
    'Admin'
)
ON CONFLICT (username) DO NOTHING;
