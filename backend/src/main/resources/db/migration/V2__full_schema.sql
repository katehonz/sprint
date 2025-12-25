-- V2: Full database schema for SP-AC accounting system
-- Bulgarian accounting system with VAT, Fixed Assets, Inventory support

-- =====================================================
-- USER MANAGEMENT
-- =====================================================

-- User groups (roles)
CREATE TABLE IF NOT EXISTS user_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    can_create_companies BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit_companies BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete_companies BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage_users BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_reports BOOLEAN NOT NULL DEFAULT FALSE,
    can_post_entries BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    group_id INTEGER REFERENCES user_groups(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    document_period_start DATE NOT NULL DEFAULT CURRENT_DATE,
    document_period_end DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '1 year'),
    document_period_active BOOLEAN NOT NULL DEFAULT TRUE,
    accounting_period_start DATE NOT NULL DEFAULT CURRENT_DATE,
    accounting_period_end DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '1 year'),
    accounting_period_active BOOLEAN NOT NULL DEFAULT TRUE,
    vat_period_start DATE NOT NULL DEFAULT CURRENT_DATE,
    vat_period_end DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '1 year'),
    vat_period_active BOOLEAN NOT NULL DEFAULT TRUE,
    recovery_code_hash VARCHAR(255),
    recovery_code_created_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- CURRENCIES & EXCHANGE RATES
-- =====================================================

CREATE TABLE IF NOT EXISTS currencies (
    id SERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    name_bg VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INTEGER NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_base_currency BOOLEAN NOT NULL DEFAULT FALSE,
    bnb_code VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id SERIAL PRIMARY KEY,
    from_currency_id INTEGER NOT NULL REFERENCES currencies(id),
    to_currency_id INTEGER NOT NULL REFERENCES currencies(id),
    rate NUMERIC(19, 4) NOT NULL,
    reverse_rate NUMERIC(19, 4) NOT NULL,
    valid_date DATE NOT NULL,
    rate_source VARCHAR(20) NOT NULL, -- ECB, MANUAL, API
    bnb_rate_id VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_currency_id, to_currency_id, valid_date)
);

CREATE INDEX idx_exchange_rates_date ON exchange_rates(valid_date);
CREATE INDEX idx_exchange_rates_currencies ON exchange_rates(from_currency_id, to_currency_id);

-- =====================================================
-- COMPANIES
-- =====================================================

CREATE TABLE IF NOT EXISTS companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    eik VARCHAR(20) NOT NULL UNIQUE,
    vat_number VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'България',
    phone VARCHAR(50),
    email VARCHAR(255),
    contact_person VARCHAR(255),
    manager_name VARCHAR(255),
    authorized_person VARCHAR(255),
    manager_egn VARCHAR(20),
    authorized_person_egn VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    contragent_api_url VARCHAR(500),
    contragent_api_key VARCHAR(255),
    enable_vies_validation BOOLEAN NOT NULL DEFAULT FALSE,
    enable_ai_mapping BOOLEAN NOT NULL DEFAULT FALSE,
    auto_validate_on_import BOOLEAN NOT NULL DEFAULT FALSE,
    base_currency_id INTEGER REFERENCES currencies(id),
    preferred_rate_provider VARCHAR(20) NOT NULL DEFAULT 'ECB', -- ECB, MANUAL, API
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_eik ON companies(eik);

-- =====================================================
-- CHART OF ACCOUNTS
-- =====================================================

CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    account_class INTEGER NOT NULL,
    parent_id INTEGER REFERENCES accounts(id),
    level INTEGER NOT NULL DEFAULT 1,
    is_vat_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    vat_direction VARCHAR(10) NOT NULL DEFAULT 'NONE', -- NONE, INPUT, OUTPUT, BOTH
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_analytical BOOLEAN NOT NULL DEFAULT FALSE,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    supports_quantities BOOLEAN NOT NULL DEFAULT FALSE,
    default_unit VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, code)
);

CREATE INDEX idx_accounts_company ON accounts(company_id);
CREATE INDEX idx_accounts_parent ON accounts(parent_id);
CREATE INDEX idx_accounts_code ON accounts(company_id, code);

-- =====================================================
-- COUNTERPARTS (Контрагенти)
-- =====================================================

CREATE TABLE IF NOT EXISTS counterparts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    eik VARCHAR(20),
    vat_number VARCHAR(20),
    street TEXT,
    address TEXT,
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    contact_person VARCHAR(255),
    counterpart_type VARCHAR(20) NOT NULL, -- CUSTOMER, SUPPLIER, EMPLOYEE, BANK, GOVERNMENT, OTHER
    is_customer BOOLEAN NOT NULL DEFAULT FALSE,
    is_supplier BOOLEAN NOT NULL DEFAULT FALSE,
    is_vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_counterparts_company ON counterparts(company_id);
CREATE INDEX idx_counterparts_eik ON counterparts(eik);
CREATE INDEX idx_counterparts_vat ON counterparts(vat_number);

-- Global contragents (shared across companies)
CREATE TABLE IF NOT EXISTS global_contragents (
    id BIGSERIAL PRIMARY KEY,
    vat_number VARCHAR(20) NOT NULL UNIQUE,
    eik VARCHAR(20),
    registration_number VARCHAR(50),
    customer_id VARCHAR(50),
    supplier_id VARCHAR(50),
    company_name VARCHAR(255),
    company_name_bg VARCHAR(255),
    legal_form VARCHAR(100),
    status VARCHAR(50),
    address TEXT,
    long_address TEXT,
    street_name VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    contragent_type VARCHAR(50),
    iban VARCHAR(50),
    bic VARCHAR(20),
    bank_name VARCHAR(255),
    vat_valid BOOLEAN NOT NULL DEFAULT FALSE,
    eik_valid BOOLEAN NOT NULL DEFAULT FALSE,
    valid BOOLEAN NOT NULL DEFAULT FALSE,
    last_validated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_global_contragents_vat ON global_contragents(vat_number);
CREATE INDEX idx_global_contragents_eik ON global_contragents(eik);

-- =====================================================
-- VAT RATES
-- =====================================================

CREATE TABLE IF NOT EXISTS vat_rates (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    rate NUMERIC(10, 4) NOT NULL,
    vat_direction VARCHAR(10) NOT NULL, -- NONE, INPUT, OUTPUT, BOTH
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE NOT NULL,
    valid_to DATE,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vat_rates_company ON vat_rates(company_id);

-- =====================================================
-- JOURNAL ENTRIES
-- =====================================================

CREATE TABLE IF NOT EXISTS journal_entries (
    id SERIAL PRIMARY KEY,
    entry_number VARCHAR(50) NOT NULL UNIQUE,
    document_date DATE NOT NULL,
    vat_date DATE,
    accounting_date DATE NOT NULL,
    document_number VARCHAR(100),
    description TEXT NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_vat_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    is_posted BOOLEAN NOT NULL DEFAULT FALSE,
    posted_by INTEGER REFERENCES users(id),
    posted_at TIMESTAMPTZ,
    created_by INTEGER NOT NULL REFERENCES users(id),
    company_id INTEGER NOT NULL REFERENCES companies(id),
    document_type VARCHAR(50),
    vat_document_type VARCHAR(50),
    vat_purchase_operation VARCHAR(50),
    vat_sales_operation VARCHAR(50),
    vat_additional_operation VARCHAR(50),
    vat_additional_data TEXT,
    vat_rate NUMERIC(19, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_entries_company ON journal_entries(company_id);
CREATE INDEX idx_journal_entries_date ON journal_entries(accounting_date);
CREATE INDEX idx_journal_entries_posted ON journal_entries(is_posted);
CREATE INDEX idx_journal_entries_document ON journal_entries(document_number);

-- Entry lines
CREATE TABLE IF NOT EXISTS entry_lines (
    id SERIAL PRIMARY KEY,
    journal_entry_id INTEGER NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    debit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    counterpart_id INTEGER REFERENCES counterparts(id),
    currency_code VARCHAR(3),
    currency_amount NUMERIC(19, 4),
    exchange_rate NUMERIC(19, 4),
    base_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_rate_id INTEGER REFERENCES vat_rates(id),
    quantity NUMERIC(19, 4),
    unit_of_measure_code VARCHAR(20),
    description TEXT,
    line_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_entry_lines_journal ON entry_lines(journal_entry_id);
CREATE INDEX idx_entry_lines_account ON entry_lines(account_id);
CREATE INDEX idx_entry_lines_counterpart ON entry_lines(counterpart_id);

-- =====================================================
-- VAT RETURNS (ДДС декларации)
-- =====================================================

CREATE TABLE IF NOT EXISTS vat_returns (
    id SERIAL PRIMARY KEY,
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    output_vat_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    input_vat_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_to_pay NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_to_refund NUMERIC(19, 4) NOT NULL DEFAULT 0,
    base_amount20 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_amount20 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    base_amount9 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_amount9 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    base_amount0 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    exempt_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base20 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_vat20 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base_vop NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_vat_vop NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_vat_personal_use NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base9 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_vat9 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base0_art3 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base0_vod NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base0_export NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base_art21 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base_art69 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_base_exempt NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_base_no_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_base_full_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_vat_full_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_base_partial_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_vat_partial_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    purchase_vat_annual_adjustment NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_coefficient NUMERIC(19, 4) NOT NULL DEFAULT 1,
    total_deductible_vat NUMERIC(19, 4) NOT NULL DEFAULT 0,
    sales_document_count INTEGER NOT NULL DEFAULT 0,
    purchase_document_count INTEGER NOT NULL DEFAULT 0,
    submitted_by_person VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, CALCULATED, SUBMITTED, ACCEPTED, REJECTED
    submitted_at TIMESTAMPTZ,
    submitted_by INTEGER REFERENCES users(id),
    due_date DATE NOT NULL,
    notes TEXT,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    created_by INTEGER NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, period_year, period_month)
);

CREATE INDEX idx_vat_returns_company ON vat_returns(company_id);
CREATE INDEX idx_vat_returns_period ON vat_returns(period_year, period_month);

-- =====================================================
-- BANKING
-- =====================================================

CREATE TABLE IF NOT EXISTS bank_profiles (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    iban VARCHAR(50),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    buffer_account_id INTEGER NOT NULL REFERENCES accounts(id),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'EUR',
    import_format VARCHAR(30) NOT NULL, -- UNICREDIT_MT940, WISE_CAMT053, etc.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    settings JSONB,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bank_profiles_company ON bank_profiles(company_id);
CREATE INDEX idx_bank_profiles_iban ON bank_profiles(iban);

CREATE TABLE IF NOT EXISTS bank_imports (
    id SERIAL PRIMARY KEY,
    bank_profile_id INTEGER NOT NULL REFERENCES bank_profiles(id),
    company_id INTEGER NOT NULL REFERENCES companies(id),
    file_name VARCHAR(500) NOT NULL,
    import_format VARCHAR(30) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transactions_count INTEGER NOT NULL DEFAULT 0,
    total_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_debit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_journal_entries INTEGER NOT NULL DEFAULT 0,
    journal_entry_ids JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- COMPLETED, FAILED, IN_PROGRESS
    error_message TEXT,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bank_imports_profile ON bank_imports(bank_profile_id);
CREATE INDEX idx_bank_imports_company ON bank_imports(company_id);

-- =====================================================
-- FIXED ASSETS (Дълготрайни активи)
-- =====================================================

CREATE TABLE IF NOT EXISTS fixed_asset_categories (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tax_category INTEGER NOT NULL,
    max_tax_depreciation_rate NUMERIC(19, 4) NOT NULL,
    default_accounting_depreciation_rate NUMERIC(19, 4),
    min_useful_life INTEGER,
    max_useful_life INTEGER,
    asset_account_code VARCHAR(20) NOT NULL,
    depreciation_account_code VARCHAR(20) NOT NULL,
    expense_account_code VARCHAR(20) NOT NULL,
    improvement_debit_account_code VARCHAR(20),
    improvement_credit_account_code VARCHAR(20),
    impairment_debit_account_code VARCHAR(20),
    impairment_credit_account_code VARCHAR(20),
    disposal_debit_account_code VARCHAR(20),
    disposal_credit_account_code VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fixed_assets (
    id SERIAL PRIMARY KEY,
    inventory_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id INTEGER NOT NULL REFERENCES fixed_asset_categories(id),
    company_id INTEGER NOT NULL REFERENCES companies(id),
    acquisition_cost NUMERIC(19, 4) NOT NULL,
    acquisition_date DATE NOT NULL,
    put_into_service_date DATE,
    accounting_useful_life INTEGER NOT NULL,
    accounting_depreciation_rate NUMERIC(19, 4) NOT NULL,
    accounting_depreciation_method VARCHAR(20) NOT NULL DEFAULT 'LINEAR',
    accounting_salvage_value NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accounting_accumulated_depreciation NUMERIC(19, 4) NOT NULL DEFAULT 0,
    tax_useful_life INTEGER,
    tax_depreciation_rate NUMERIC(19, 4) NOT NULL,
    tax_accumulated_depreciation NUMERIC(19, 4) NOT NULL DEFAULT 0,
    is_new_first_time_investment BOOLEAN NOT NULL DEFAULT FALSE,
    accounting_book_value NUMERIC(19, 4) NOT NULL,
    tax_book_value NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISPOSED, WRITTEN_OFF
    disposal_date DATE,
    disposal_amount NUMERIC(19, 4),
    location VARCHAR(255),
    responsible_person VARCHAR(255),
    serial_number VARCHAR(100),
    manufacturer VARCHAR(255),
    model VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fixed_assets_company ON fixed_assets(company_id);
CREATE INDEX idx_fixed_assets_category ON fixed_assets(category_id);
CREATE INDEX idx_fixed_assets_status ON fixed_assets(status);

-- Depreciation journal
CREATE TABLE IF NOT EXISTS depreciation_journal (
    id SERIAL PRIMARY KEY,
    fixed_asset_id INTEGER NOT NULL REFERENCES fixed_assets(id),
    period DATE NOT NULL,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    accounting_depreciation_amount NUMERIC(19, 4) NOT NULL,
    accounting_book_value_before NUMERIC(19, 4) NOT NULL,
    accounting_book_value_after NUMERIC(19, 4) NOT NULL,
    tax_depreciation_amount NUMERIC(19, 4) NOT NULL,
    tax_book_value_before NUMERIC(19, 4) NOT NULL,
    tax_book_value_after NUMERIC(19, 4) NOT NULL,
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    is_posted BOOLEAN NOT NULL DEFAULT FALSE,
    posted_at TIMESTAMPTZ,
    posted_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_depreciation_journal_asset ON depreciation_journal(fixed_asset_id);
CREATE INDEX idx_depreciation_journal_period ON depreciation_journal(period);

-- Asset value adjustments
CREATE TABLE IF NOT EXISTS asset_value_adjustments (
    id SERIAL PRIMARY KEY,
    fixed_asset_id INTEGER NOT NULL REFERENCES fixed_assets(id),
    adjustment_date DATE NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    book_value_before NUMERIC(19, 4) NOT NULL,
    book_value_after NUMERIC(19, 4) NOT NULL,
    reason TEXT,
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INVENTORY (Складови наличности)
-- =====================================================

CREATE TABLE IF NOT EXISTS inventory_balances (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    current_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    current_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    current_average_cost NUMERIC(19, 4) NOT NULL DEFAULT 0,
    last_movement_date DATE,
    last_movement_id INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, account_id)
);

CREATE INDEX idx_inventory_balances_company ON inventory_balances(company_id);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    entry_line_id INTEGER NOT NULL REFERENCES entry_lines(id),
    journal_entry_id INTEGER NOT NULL REFERENCES journal_entries(id),
    movement_date DATE NOT NULL,
    movement_type VARCHAR(20) NOT NULL, -- IN, OUT, TRANSFER
    quantity NUMERIC(19, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    unit_of_measure VARCHAR(20),
    description TEXT,
    balance_after_quantity NUMERIC(19, 4) NOT NULL,
    balance_after_amount NUMERIC(19, 4) NOT NULL,
    average_cost_at_time NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_movements_company ON inventory_movements(company_id);
CREATE INDEX idx_inventory_movements_account ON inventory_movements(account_id);
CREATE INDEX idx_inventory_movements_date ON inventory_movements(movement_date);
CREATE INDEX idx_inventory_movements_journal ON inventory_movements(journal_entry_id);

-- Average cost corrections
CREATE TABLE IF NOT EXISTS average_cost_corrections (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    correction_date DATE NOT NULL,
    old_average_cost NUMERIC(19, 4) NOT NULL,
    new_average_cost NUMERIC(19, 4) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    reason TEXT,
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INTRASTAT
-- =====================================================

CREATE TABLE IF NOT EXISTS intrastat_nomenclature (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    description_bg TEXT,
    parent_code VARCHAR(20),
    level INTEGER NOT NULL DEFAULT 1,
    unit_code VARCHAR(10),
    supplementary_unit_code VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE,
    valid_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intrastat_account_mappings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    cn_code VARCHAR(20) NOT NULL,
    description TEXT,
    default_country VARCHAR(2),
    default_transport_mode INTEGER,
    default_delivery_terms VARCHAR(3),
    default_transaction_type VARCHAR(3),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, account_id)
);

CREATE TABLE IF NOT EXISTS intrastat_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) UNIQUE,
    arrival_threshold NUMERIC(19, 4),
    dispatch_threshold NUMERIC(19, 4),
    statistical_threshold NUMERIC(19, 4),
    default_transport_mode INTEGER,
    default_delivery_terms VARCHAR(3),
    include_statistical_value BOOLEAN NOT NULL DEFAULT FALSE,
    auto_generate_from_vat BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intrastat_declarations (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    declaration_type VARCHAR(20) NOT NULL, -- ARRIVAL, DISPATCH
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    reference_number VARCHAR(50),
    total_items INTEGER NOT NULL DEFAULT 0,
    total_value NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_statistical_value NUMERIC(19, 4),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMPTZ,
    submitted_by INTEGER REFERENCES users(id),
    notes TEXT,
    created_by INTEGER NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, declaration_type, period_year, period_month)
);

CREATE TABLE IF NOT EXISTS intrastat_declaration_items (
    id SERIAL PRIMARY KEY,
    declaration_id INTEGER NOT NULL REFERENCES intrastat_declarations(id) ON DELETE CASCADE,
    item_number INTEGER NOT NULL,
    cn_code VARCHAR(20) NOT NULL,
    country_of_origin VARCHAR(2),
    country_of_destination VARCHAR(2),
    region_of_origin VARCHAR(10),
    region_of_destination VARCHAR(10),
    net_mass NUMERIC(19, 4),
    supplementary_units NUMERIC(19, 4),
    invoice_value NUMERIC(19, 4) NOT NULL,
    statistical_value NUMERIC(19, 4),
    transport_mode INTEGER,
    delivery_terms VARCHAR(3),
    transaction_type VARCHAR(3),
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- AI & AUTOMATION SETTINGS
-- =====================================================

CREATE TABLE IF NOT EXISTS ai_accounting_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    document_type VARCHAR(50) NOT NULL,
    counterpart_pattern TEXT,
    description_pattern TEXT,
    default_account_id INTEGER REFERENCES accounts(id),
    default_vat_rate_id INTEGER REFERENCES vat_rates(id),
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_bank_accounting_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    bank_profile_id INTEGER REFERENCES bank_profiles(id),
    counterpart_pattern TEXT,
    description_pattern TEXT,
    amount_min NUMERIC(19, 4),
    amount_max NUMERIC(19, 4),
    is_credit BOOLEAN,
    default_account_id INTEGER REFERENCES accounts(id),
    default_counterpart_id INTEGER REFERENCES counterparts(id),
    default_description TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contragent_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    counterpart_id INTEGER NOT NULL REFERENCES counterparts(id),
    default_purchase_account_id INTEGER REFERENCES accounts(id),
    default_sales_account_id INTEGER REFERENCES accounts(id),
    default_vat_rate_id INTEGER REFERENCES vat_rates(id),
    payment_terms_days INTEGER,
    credit_limit NUMERIC(19, 4),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, counterpart_id)
);

-- =====================================================
-- CURRENCY REVALUATION
-- =====================================================

CREATE TABLE IF NOT EXISTS currency_revaluation_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) UNIQUE,
    revaluation_account_id INTEGER REFERENCES accounts(id),
    positive_difference_account_id INTEGER REFERENCES accounts(id),
    negative_difference_account_id INTEGER REFERENCES accounts(id),
    revalue_receivables BOOLEAN NOT NULL DEFAULT TRUE,
    revalue_payables BOOLEAN NOT NULL DEFAULT TRUE,
    revalue_bank_accounts BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- CONTROLISY IMPORTS
-- =====================================================

CREATE TABLE IF NOT EXISTS controlisy_imports (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    file_name VARCHAR(500) NOT NULL,
    import_type VARCHAR(50) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    records_count INTEGER NOT NULL DEFAULT 0,
    created_entries INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PRODUCTION (Производство)
-- =====================================================

CREATE TABLE IF NOT EXISTS technology_cards (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    output_account_id INTEGER NOT NULL REFERENCES accounts(id),
    output_quantity NUMERIC(19, 4) NOT NULL DEFAULT 1,
    output_unit VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, code)
);

CREATE TABLE IF NOT EXISTS technology_card_stages (
    id SERIAL PRIMARY KEY,
    technology_card_id INTEGER NOT NULL REFERENCES technology_cards(id) ON DELETE CASCADE,
    stage_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    input_account_id INTEGER NOT NULL REFERENCES accounts(id),
    input_quantity NUMERIC(19, 4) NOT NULL,
    input_unit VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS production_batches (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    technology_card_id INTEGER NOT NULL REFERENCES technology_cards(id),
    batch_number VARCHAR(50) NOT NULL,
    planned_quantity NUMERIC(19, 4) NOT NULL,
    actual_quantity NUMERIC(19, 4),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    notes TEXT,
    created_by INTEGER REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, batch_number)
);

CREATE TABLE IF NOT EXISTS production_batch_stages (
    id SERIAL PRIMARY KEY,
    production_batch_id INTEGER NOT NULL REFERENCES production_batches(id) ON DELETE CASCADE,
    technology_card_stage_id INTEGER NOT NULL REFERENCES technology_card_stages(id),
    planned_quantity NUMERIC(19, 4) NOT NULL,
    actual_quantity NUMERIC(19, 4),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    journal_entry_id INTEGER REFERENCES journal_entries(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INITIAL DATA
-- =====================================================

-- Insert default user group
INSERT INTO user_groups (name, description, can_create_companies, can_edit_companies, can_delete_companies, can_manage_users, can_view_reports, can_post_entries)
VALUES ('ADMIN', 'Администратор', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Insert default currencies (EUR as base for Bulgaria from 2025)
INSERT INTO currencies (code, name, name_bg, symbol, decimal_places, is_base_currency)
VALUES
    ('EUR', 'Euro', 'Евро', '€', 2, TRUE),
    ('BGN', 'Bulgarian Lev', 'Български лев', 'лв', 2, FALSE),
    ('USD', 'US Dollar', 'Щатски долар', '$', 2, FALSE),
    ('GBP', 'British Pound', 'Британска лира', '£', 2, FALSE),
    ('CHF', 'Swiss Franc', 'Швейцарски франк', 'CHF', 2, FALSE)
ON CONFLICT (code) DO NOTHING;

-- Insert fixed BGN/EUR rate (1.95583)
INSERT INTO exchange_rates (from_currency_id, to_currency_id, rate, reverse_rate, valid_date, rate_source)
SELECT
    (SELECT id FROM currencies WHERE code = 'BGN'),
    (SELECT id FROM currencies WHERE code = 'EUR'),
    0.511292,
    1.95583,
    CURRENT_DATE,
    'ECB'
WHERE EXISTS (SELECT 1 FROM currencies WHERE code = 'BGN')
  AND EXISTS (SELECT 1 FROM currencies WHERE code = 'EUR')
ON CONFLICT (from_currency_id, to_currency_id, valid_date) DO NOTHING;

-- Insert default fixed asset categories (Bulgarian tax categories)
INSERT INTO fixed_asset_categories (code, name, tax_category, max_tax_depreciation_rate, asset_account_code, depreciation_account_code, expense_account_code)
VALUES
    ('CAT1', 'Компютри и периферни устройства', 1, 50.00, '2040', '2049', '6030'),
    ('CAT2', 'Автомобили', 2, 25.00, '2050', '2059', '6030'),
    ('CAT3', 'Машини и оборудване', 3, 15.00, '2030', '2039', '6030'),
    ('CAT4', 'Сгради и конструкции', 4, 4.00, '2010', '2019', '6030'),
    ('CAT5', 'Други дълготрайни активи', 5, 15.00, '2090', '2099', '6030'),
    ('CAT6', 'Нематериални активи', 6, 33.33, '2110', '2119', '6030'),
    ('CAT7', 'Права върху интелектуална собственост', 7, 15.00, '2120', '2129', '6030')
ON CONFLICT (code) DO NOTHING;

-- Insert default VAT rates (Bulgarian)
-- Note: This requires a company_id, so it should be done after company creation
-- These are just example rates that would be created per company
