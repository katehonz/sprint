# База данни

PostgreSQL схема за счетоводната система.

## Основни таблици

### companies
```sql
CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    eik VARCHAR(13) UNIQUE,
    vat_number VARCHAR(15),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(2) DEFAULT 'BG',
    phone VARCHAR(20),
    email VARCHAR(255),
    contact_person VARCHAR(255),
    manager_name VARCHAR(255),
    authorized_person VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    enable_vies_validation BOOLEAN DEFAULT false,
    enable_ai_mapping BOOLEAN DEFAULT false,
    preferred_rate_provider VARCHAR(10) DEFAULT 'ECB',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### accounts (Сметкоплан)
```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20), -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    account_class INTEGER,    -- 1-7
    level INTEGER DEFAULT 1,
    parent_id UUID REFERENCES accounts(id),
    is_analytical BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    supports_quantities BOOLEAN DEFAULT false,
    default_unit VARCHAR(10),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(company_id, code)
);
```

### journal_entries (Журнални записи)
```sql
CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    entry_number VARCHAR(20) NOT NULL,
    document_date DATE NOT NULL,
    vat_date DATE,
    accounting_date DATE NOT NULL,
    document_number VARCHAR(50),
    description TEXT,
    document_type VARCHAR(2),
    counterpart_id UUID REFERENCES counterparts(id),
    total_amount DECIMAL(15,2),
    total_vat_amount DECIMAL(15,2),
    currency_code VARCHAR(3) DEFAULT 'EUR',
    is_posted BOOLEAN DEFAULT false,
    posted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(company_id, entry_number)
);
```

### journal_entry_lines (Редове)
```sql
CREATE TABLE journal_entry_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID REFERENCES journal_entries(id),
    account_id UUID REFERENCES accounts(id),
    debit_amount DECIMAL(15,2),
    credit_amount DECIMAL(15,2),
    counterpart_id UUID REFERENCES counterparts(id),
    currency_code VARCHAR(3) DEFAULT 'EUR',
    exchange_rate DECIMAL(15,6) DEFAULT 1,
    base_amount DECIMAL(15,2),
    vat_amount DECIMAL(15,2),
    quantity DECIMAL(15,4),
    unit_of_measure VARCHAR(10),
    description TEXT,
    line_order INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### counterparts (Контрагенти)
```sql
CREATE TABLE counterparts (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    eik VARCHAR(13),
    vat_number VARCHAR(15),
    address TEXT,
    long_address VARCHAR(500),  -- Адрес от VIES (един дълъг string)
    city VARCHAR(100),
    country VARCHAR(2) DEFAULT 'BG',
    phone VARCHAR(20),
    email VARCHAR(255),
    counterpart_type VARCHAR(20), -- CUSTOMER, SUPPLIER, EMPLOYEE, BANK, GOVERNMENT, OTHER
    is_vat_registered BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Забележка:** Полето `long_address` съхранява пълния адрес, както се връща от VIES системата - като един дълъг string без разделяне на компоненти (улица, град, пощенски код и т.н.).

### currencies (Валути)
```sql
CREATE TABLE currencies (
    id UUID PRIMARY KEY,
    code VARCHAR(3) UNIQUE NOT NULL,
    name VARCHAR(100),
    name_bg VARCHAR(100),
    symbol VARCHAR(5),
    decimal_places INTEGER DEFAULT 2,
    is_active BOOLEAN DEFAULT true,
    is_base_currency BOOLEAN DEFAULT false
);
```

### exchange_rates (Курсове)
```sql
CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY,
    from_currency_code VARCHAR(3) REFERENCES currencies(code),
    to_currency_code VARCHAR(3) REFERENCES currencies(code),
    rate DECIMAL(15,6) NOT NULL,
    reverse_rate DECIMAL(15,6),
    valid_date DATE NOT NULL,
    rate_source VARCHAR(10) DEFAULT 'ECB',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(from_currency_code, to_currency_code, valid_date, rate_source)
);
```

### vat_rates (ДДС ставки)
```sql
CREATE TABLE vat_rates (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    code VARCHAR(10) NOT NULL,
    name VARCHAR(100),
    rate DECIMAL(5,2) NOT NULL,
    vat_direction VARCHAR(10), -- INPUT, OUTPUT
    is_active BOOLEAN DEFAULT true,
    valid_from DATE,
    valid_to DATE,
    UNIQUE(company_id, code)
);
```

### vat_returns (ДДС декларации)
```sql
CREATE TABLE vat_returns (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    period_from DATE,
    period_to DATE,
    output_vat_amount DECIMAL(15,2),
    input_vat_amount DECIMAL(15,2),
    vat_to_pay DECIMAL(15,2),
    vat_to_refund DECIMAL(15,2),
    status VARCHAR(20) DEFAULT 'DRAFT',
    due_date DATE,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(company_id, period_year, period_month)
);
```

### fixed_assets (ДМА)
```sql
CREATE TABLE fixed_assets (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    inventory_number VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category_id UUID REFERENCES fixed_asset_categories(id),
    acquisition_cost DECIMAL(15,2),
    acquisition_date DATE,
    put_into_service_date DATE,
    accounting_depreciation_rate DECIMAL(5,2),
    tax_depreciation_rate DECIMAL(5,2),
    accounting_accumulated_depreciation DECIMAL(15,2) DEFAULT 0,
    accounting_book_value DECIMAL(15,2),
    tax_accumulated_depreciation DECIMAL(15,2) DEFAULT 0,
    tax_book_value DECIMAL(15,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(company_id, inventory_number)
);
```

## Индекси

```sql
CREATE INDEX idx_journal_entries_company ON journal_entries(company_id);
CREATE INDEX idx_journal_entries_dates ON journal_entries(document_date, accounting_date, vat_date);
CREATE INDEX idx_accounts_company ON accounts(company_id);
CREATE INDEX idx_counterparts_company ON counterparts(company_id);
CREATE INDEX idx_exchange_rates_date ON exchange_rates(valid_date);
```

## Банкови таблици

### bank_profiles
```sql
CREATE TABLE bank_profiles (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    iban VARCHAR(50),
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    buffer_account_id INTEGER NOT NULL REFERENCES accounts(id),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'EUR',
    connection_type VARCHAR(20) NOT NULL DEFAULT 'FILE_IMPORT', -- FILE_IMPORT, SALT_EDGE, MANUAL
    import_format VARCHAR(30), -- UNICREDIT_MT940, WISE_CAMT053, etc.
    -- Salt Edge Open Banking fields
    salt_edge_connection_id VARCHAR(100),
    salt_edge_account_id VARCHAR(100),
    salt_edge_provider_code VARCHAR(100),
    salt_edge_provider_name VARCHAR(255),
    salt_edge_last_sync_at TIMESTAMPTZ,
    salt_edge_consent_expires_at TIMESTAMPTZ,
    salt_edge_status VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    settings JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### bank_imports
```sql
CREATE TABLE bank_imports (
    id SERIAL PRIMARY KEY,
    bank_profile_id INTEGER NOT NULL REFERENCES bank_profiles(id),
    company_id INTEGER NOT NULL REFERENCES companies(id),
    file_name VARCHAR(500) NOT NULL,
    import_format VARCHAR(30) NOT NULL,
    import_source VARCHAR(20) NOT NULL DEFAULT 'FILE', -- FILE, OPEN_BANKING
    imported_at TIMESTAMPTZ NOT NULL,
    transactions_count INTEGER NOT NULL DEFAULT 0,
    total_credit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_debit NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_journal_entries INTEGER NOT NULL DEFAULT 0,
    journal_entry_ids JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

## Salt Edge Open Banking таблици

### salt_edge_customers
```sql
CREATE TABLE salt_edge_customers (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) UNIQUE,
    salt_edge_customer_id VARCHAR(100) NOT NULL UNIQUE,
    identifier VARCHAR(255) NOT NULL,
    secret VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### salt_edge_connections
```sql
CREATE TABLE salt_edge_connections (
    id SERIAL PRIMARY KEY,
    bank_profile_id INTEGER NOT NULL REFERENCES bank_profiles(id),
    salt_edge_connection_id VARCHAR(100) NOT NULL UNIQUE,
    salt_edge_customer_id VARCHAR(100) NOT NULL,
    provider_id VARCHAR(100),
    provider_code VARCHAR(100) NOT NULL,
    provider_name VARCHAR(255),
    country_code VARCHAR(2),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    last_success_at TIMESTAMPTZ,
    next_refresh_possible_at TIMESTAMPTZ,
    daily_refresh BOOLEAN DEFAULT FALSE,
    consent_id VARCHAR(100),
    consent_expires_at TIMESTAMPTZ,
    error_class VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### salt_edge_accounts
```sql
CREATE TABLE salt_edge_accounts (
    id SERIAL PRIMARY KEY,
    salt_edge_connection_id VARCHAR(100) NOT NULL,
    salt_edge_account_id VARCHAR(100) NOT NULL UNIQUE,
    bank_profile_id INTEGER REFERENCES bank_profiles(id),
    name VARCHAR(255) NOT NULL,
    nature VARCHAR(50), -- checking, savings, credit_card
    currency_code VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4),
    available_amount NUMERIC(19, 4),
    iban VARCHAR(50),
    swift VARCHAR(20),
    account_number VARCHAR(100),
    is_mapped BOOLEAN NOT NULL DEFAULT FALSE,
    extra_data JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### salt_edge_transactions
```sql
CREATE TABLE salt_edge_transactions (
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
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

## Flyway миграции

Миграциите са в `src/main/resources/db/migration/`:

- V1__init.sql - Начална схема
- V2__full_schema.sql - Пълна схема с всички таблици
- V3__add_azure_keys_to_companies.sql - Azure Form Recognizer ключове
- V4__salt_edge_open_banking.sql - Salt Edge Open Banking таблици
- V5__salt_edge_company_keys.sql - Salt Edge ключове per company
- V6__add_long_address_to_counterparts.sql - Поле за адрес от VIES

## ER диаграма (Banking)

```
┌─────────────────┐       ┌──────────────────────┐
│    companies    │       │  salt_edge_customers │
│─────────────────│       │──────────────────────│
│ id              │───────│ company_id           │
│ salt_edge_app_id│       │ salt_edge_customer_id│
│ salt_edge_secret│       │ identifier           │
│ salt_edge_enabled       └──────────────────────┘
└────────┬────────┘                │
         │                         │
         │                         ▼
┌────────▼────────┐       ┌──────────────────────┐
│  bank_profiles  │◄──────│ salt_edge_connections│
│─────────────────│       │──────────────────────│
│ id              │       │ bank_profile_id      │
│ connection_type │       │ salt_edge_conn_id    │
│ import_format   │       │ provider_code        │
│ salt_edge_*     │       │ status               │
└────────┬────────┘       └──────────────────────┘
         │                         │
         │                         ▼
┌────────▼────────┐       ┌──────────────────────┐
│  bank_imports   │       │  salt_edge_accounts  │
│─────────────────│       │──────────────────────│
│ bank_profile_id │       │ salt_edge_conn_id    │
│ import_source   │       │ bank_profile_id      │
│ status          │       │ iban                 │
└─────────────────┘       │ is_mapped            │
                          └──────────────────────┘
                                   │
                                   ▼
                          ┌──────────────────────┐
                          │salt_edge_transactions│
                          │──────────────────────│
                          │ salt_edge_account_id │
                          │ bank_profile_id      │
                          │ is_processed         │
                          │ journal_entry_id     │
                          └──────────────────────┘
```
