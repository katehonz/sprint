ALTER TABLE companies ADD COLUMN nap_office VARCHAR(255);
COMMENT ON COLUMN companies.nap_office IS 'ТД на НАП (код)';
