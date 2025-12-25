-- Add long_address column to counterparts table
-- This stores the full address as returned by VIES (single line format)
ALTER TABLE counterparts ADD COLUMN IF NOT EXISTS long_address VARCHAR(500);

COMMENT ON COLUMN counterparts.long_address IS 'Full address as returned by VIES validation (single line format)';
