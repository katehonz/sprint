-- Add document number and date fields to fixed_assets table
ALTER TABLE fixed_assets ADD COLUMN IF NOT EXISTS document_number VARCHAR(50);
ALTER TABLE fixed_assets ADD COLUMN IF NOT EXISTS document_date DATE;
