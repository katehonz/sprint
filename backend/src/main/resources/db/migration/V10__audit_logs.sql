-- Audit Logs - записване на действията на потребителите
-- 6 месеца ретенция, достъп само за SUPER_ADMIN

CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    company_id INTEGER REFERENCES companies(id),
    user_id INTEGER REFERENCES users(id),

    -- Потребителска информация (денормализирана за бързодействие)
    username VARCHAR(100),
    user_role VARCHAR(50),

    -- Какво действие
    action VARCHAR(50) NOT NULL,      -- LOGIN, LOGOUT, VIEW_REPORT, GENERATE_REPORT, EXPORT, CREATE, UPDATE, DELETE
    entity_type VARCHAR(50),          -- JOURNAL_ENTRY, VAT_RETURN, COUNTERPART, REPORT, etc.
    entity_id VARCHAR(50),            -- ID на записа (може да е и стринг)

    -- Детайли в JSON формат
    details JSONB,                    -- { "reportType": "turnover", "period": "2025-01", "filters": {...} }

    -- Мрежова информация
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- Резултат
    success BOOLEAN DEFAULT true,
    error_message TEXT,

    -- Време
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Индекси за бързо търсене
CREATE INDEX idx_audit_company ON audit_logs(company_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_details ON audit_logs USING GIN (details);

-- Настройки за логване по роля/потребител
CREATE TABLE audit_settings (
    id SERIAL PRIMARY KEY,
    company_id INTEGER REFERENCES companies(id),
    role VARCHAR(50),                 -- SUPER_ADMIN, ADMIN, MANAGER, ACCOUNTANT, VIEWER (NULL = глобално)
    user_id INTEGER REFERENCES users(id),  -- NULL = за цялата роля

    -- Кои действия да се логват
    log_login BOOLEAN DEFAULT true,
    log_logout BOOLEAN DEFAULT true,
    log_view_reports BOOLEAN DEFAULT true,
    log_generate_reports BOOLEAN DEFAULT true,
    log_export_data BOOLEAN DEFAULT true,
    log_create BOOLEAN DEFAULT false,
    log_update BOOLEAN DEFAULT false,
    log_delete BOOLEAN DEFAULT true,

    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    UNIQUE(company_id, role, user_id)
);

-- Вмъкване на настройки по подразбиране - логваме всичко за MANAGER и OWNER роли
INSERT INTO audit_settings (company_id, role, log_login, log_logout, log_view_reports, log_generate_reports, log_export_data, log_create, log_update, log_delete)
VALUES
    (NULL, 'SUPER_ADMIN', true, true, true, true, true, true, true, true),
    (NULL, 'ADMIN', true, true, true, true, true, false, false, true),
    (NULL, 'MANAGER', true, true, true, true, true, false, false, false),
    (NULL, 'OWNER', true, true, true, true, true, false, false, false);

-- Функция за автоматично изтриване на стари логове (над 6 месеца)
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM audit_logs
    WHERE created_at < NOW() - INTERVAL '6 months';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Коментар за документация
COMMENT ON TABLE audit_logs IS 'Одит логове за проследяване на действията на потребителите. Пазят се 6 месеца.';
COMMENT ON TABLE audit_settings IS 'Настройки кои действия да се логват за различните роли и потребители.';
