-- Accounting periods table for period locking functionality
CREATE TABLE IF NOT EXISTS accounting_periods (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_by INTEGER REFERENCES users(id),
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, year, month)
);

CREATE INDEX idx_accounting_periods_company ON accounting_periods(company_id);
CREATE INDEX idx_accounting_periods_year_month ON accounting_periods(year, month);
CREATE INDEX idx_accounting_periods_status ON accounting_periods(status);

COMMENT ON TABLE accounting_periods IS 'Счетоводни периоди за заключване след подаване на ДДС декларации';
COMMENT ON COLUMN accounting_periods.status IS 'OPEN - може да се редактира, CLOSED - заключен период';
