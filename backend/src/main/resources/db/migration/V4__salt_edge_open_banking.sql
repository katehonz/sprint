-- V4: Salt Edge Open Banking integration
-- Adds support for Open Banking alongside file-based imports

-- Add connection type to bank_profiles
ALTER TABLE bank_profiles
ADD COLUMN IF NOT EXISTS connection_type VARCHAR(20) NOT NULL DEFAULT 'FILE_IMPORT';

-- Salt Edge specific fields for bank_profiles
ALTER TABLE bank_profiles
ADD COLUMN IF NOT EXISTS salt_edge_connection_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS salt_edge_account_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS salt_edge_provider_code VARCHAR(100),
ADD COLUMN IF NOT EXISTS salt_edge_provider_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS salt_edge_last_sync_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS salt_edge_consent_expires_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS salt_edge_status VARCHAR(50);

-- Make import_format nullable (not needed for Open Banking)
ALTER TABLE bank_profiles
ALTER COLUMN import_format DROP NOT NULL;

-- Salt Edge customers table (one per company)
CREATE TABLE IF NOT EXISTS salt_edge_customers (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) UNIQUE,
    salt_edge_customer_id VARCHAR(100) NOT NULL UNIQUE,
    identifier VARCHAR(255) NOT NULL,
    secret VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salt_edge_customers_company ON salt_edge_customers(company_id);
CREATE INDEX idx_salt_edge_customers_se_id ON salt_edge_customers(salt_edge_customer_id);

-- Salt Edge connections table (detailed connection info)
CREATE TABLE IF NOT EXISTS salt_edge_connections (
    id SERIAL PRIMARY KEY,
    bank_profile_id INTEGER NOT NULL REFERENCES bank_profiles(id),
    salt_edge_connection_id VARCHAR(100) NOT NULL UNIQUE,
    salt_edge_customer_id VARCHAR(100) NOT NULL,
    provider_id VARCHAR(100),
    provider_code VARCHAR(100) NOT NULL,
    provider_name VARCHAR(255),
    country_code VARCHAR(2),
    status VARCHAR(50) NOT NULL DEFAULT 'pending', -- active, inactive, disabled, pending
    last_success_at TIMESTAMPTZ,
    next_refresh_possible_at TIMESTAMPTZ,
    daily_refresh BOOLEAN DEFAULT FALSE,
    consent_id VARCHAR(100),
    consent_expires_at TIMESTAMPTZ,
    error_class VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salt_edge_connections_profile ON salt_edge_connections(bank_profile_id);
CREATE INDEX idx_salt_edge_connections_se_id ON salt_edge_connections(salt_edge_connection_id);
CREATE INDEX idx_salt_edge_connections_status ON salt_edge_connections(status);

-- Salt Edge accounts table (accounts within a connection)
CREATE TABLE IF NOT EXISTS salt_edge_accounts (
    id SERIAL PRIMARY KEY,
    salt_edge_connection_id VARCHAR(100) NOT NULL,
    salt_edge_account_id VARCHAR(100) NOT NULL UNIQUE,
    bank_profile_id INTEGER REFERENCES bank_profiles(id),
    name VARCHAR(255) NOT NULL,
    nature VARCHAR(50), -- checking, savings, credit_card, etc.
    currency_code VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4),
    available_amount NUMERIC(19, 4),
    iban VARCHAR(50),
    swift VARCHAR(20),
    account_number VARCHAR(100),
    is_mapped BOOLEAN NOT NULL DEFAULT FALSE,
    extra_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salt_edge_accounts_connection ON salt_edge_accounts(salt_edge_connection_id);
CREATE INDEX idx_salt_edge_accounts_profile ON salt_edge_accounts(bank_profile_id);
CREATE INDEX idx_salt_edge_accounts_iban ON salt_edge_accounts(iban);

-- Salt Edge transactions table (raw transactions before processing)
CREATE TABLE IF NOT EXISTS salt_edge_transactions (
    id SERIAL PRIMARY KEY,
    salt_edge_account_id VARCHAR(100) NOT NULL,
    salt_edge_transaction_id VARCHAR(100) NOT NULL UNIQUE,
    bank_profile_id INTEGER REFERENCES bank_profiles(id),
    made_on DATE NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    mode VARCHAR(50), -- normal, fee, transfer
    status VARCHAR(50) NOT NULL, -- posted, pending
    duplicated BOOLEAN DEFAULT FALSE,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    extra_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salt_edge_txn_account ON salt_edge_transactions(salt_edge_account_id);
CREATE INDEX idx_salt_edge_txn_profile ON salt_edge_transactions(bank_profile_id);
CREATE INDEX idx_salt_edge_txn_date ON salt_edge_transactions(made_on);
CREATE INDEX idx_salt_edge_txn_processed ON salt_edge_transactions(is_processed);
CREATE INDEX idx_salt_edge_txn_se_id ON salt_edge_transactions(salt_edge_transaction_id);

-- Add source column to bank_imports to track import source
ALTER TABLE bank_imports
ADD COLUMN IF NOT EXISTS import_source VARCHAR(20) NOT NULL DEFAULT 'FILE';

-- Comments for documentation
COMMENT ON TABLE salt_edge_customers IS 'Salt Edge customer records - one per company';
COMMENT ON TABLE salt_edge_connections IS 'Salt Edge bank connections - detailed connection status';
COMMENT ON TABLE salt_edge_accounts IS 'Salt Edge bank accounts - accounts within connections';
COMMENT ON TABLE salt_edge_transactions IS 'Salt Edge transactions - raw transactions before journal entry creation';
COMMENT ON COLUMN bank_profiles.connection_type IS 'Type of connection: FILE_IMPORT, SALT_EDGE, or MANUAL';
COMMENT ON COLUMN bank_imports.import_source IS 'Source of import: FILE or OPEN_BANKING';
