-- Add vatRefundArt92 field to vat_returns table for manual input of cell 82
ALTER TABLE vat_returns ADD COLUMN IF NOT EXISTS vat_refund_art92 DECIMAL(19, 4);
