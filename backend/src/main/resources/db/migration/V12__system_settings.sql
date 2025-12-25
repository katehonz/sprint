-- Глобални системни настройки (единствен запис)

CREATE TABLE system_settings (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1), -- Гарантира единствен запис

    -- SMTP настройки
    smtp_host VARCHAR(255),
    smtp_port INTEGER DEFAULT 587,
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(255), -- TODO: Encrypt
    smtp_from_email VARCHAR(255),
    smtp_from_name VARCHAR(255),
    smtp_use_tls BOOLEAN NOT NULL DEFAULT TRUE,
    smtp_use_ssl BOOLEAN NOT NULL DEFAULT FALSE,
    smtp_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Други глобални настройки
    app_name VARCHAR(255) DEFAULT 'SP-AC Accounting',
    default_language VARCHAR(10) DEFAULT 'bg',
    default_timezone VARCHAR(50) DEFAULT 'Europe/Sofia',

    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Създаваме начален запис
INSERT INTO system_settings (id, smtp_port, smtp_use_tls, smtp_use_ssl, smtp_enabled, app_name, default_language, default_timezone)
VALUES (1, 587, TRUE, FALSE, FALSE, 'SP-AC Accounting', 'bg', 'Europe/Sofia');

COMMENT ON TABLE system_settings IS 'Глобални системни настройки - единствен запис';
COMMENT ON COLUMN system_settings.smtp_host IS 'SMTP сървър адрес';
COMMENT ON COLUMN system_settings.smtp_port IS 'SMTP порт (обикновено 587 за TLS или 465 за SSL)';
COMMENT ON COLUMN system_settings.smtp_username IS 'SMTP потребителско име';
COMMENT ON COLUMN system_settings.smtp_password IS 'SMTP парола (криптирана)';
COMMENT ON COLUMN system_settings.smtp_from_email IS 'Email адрес на изпращача';
COMMENT ON COLUMN system_settings.smtp_from_name IS 'Име на изпращача';
COMMENT ON COLUMN system_settings.smtp_use_tls IS 'Използване на STARTTLS';
COMMENT ON COLUMN system_settings.smtp_use_ssl IS 'Използване на SSL/TLS';
COMMENT ON COLUMN system_settings.smtp_enabled IS 'SMTP изпращане активирано';
