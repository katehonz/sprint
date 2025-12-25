-- V5: Salt Edge API keys per company
-- Each company stores their own Salt Edge credentials

ALTER TABLE companies
ADD COLUMN IF NOT EXISTS salt_edge_app_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS salt_edge_secret VARCHAR(500),
ADD COLUMN IF NOT EXISTS salt_edge_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN companies.salt_edge_app_id IS 'Salt Edge App ID за Open Banking - всяка компания има собствен акаунт';
COMMENT ON COLUMN companies.salt_edge_secret IS 'Salt Edge Secret за Open Banking';
COMMENT ON COLUMN companies.salt_edge_enabled IS 'Дали Salt Edge е активиран за тази компания';
