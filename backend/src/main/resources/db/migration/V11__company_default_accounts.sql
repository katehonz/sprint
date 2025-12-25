-- Добавяне на default сметки за автоматични операции в companies

ALTER TABLE companies
    ADD COLUMN default_cash_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_customers_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_suppliers_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_sales_revenue_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_vat_purchase_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_vat_sales_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_card_payment_purchase_account_id INTEGER REFERENCES accounts(id),
    ADD COLUMN default_card_payment_sales_account_id INTEGER REFERENCES accounts(id);

COMMENT ON COLUMN companies.default_cash_account_id IS 'Каса - плащания в брой (обикновено 501)';
COMMENT ON COLUMN companies.default_customers_account_id IS 'Клиенти (обикновено 411)';
COMMENT ON COLUMN companies.default_suppliers_account_id IS 'Доставчици (обикновено 401)';
COMMENT ON COLUMN companies.default_sales_revenue_account_id IS 'Приходи от продажби - дефолт (обикновено 702 или 703)';
COMMENT ON COLUMN companies.default_vat_purchase_account_id IS 'ДДС на покупките (обикновено 4531)';
COMMENT ON COLUMN companies.default_vat_sales_account_id IS 'ДДС на продажбите (обикновено 4532)';
COMMENT ON COLUMN companies.default_card_payment_purchase_account_id IS 'Плащания с карта при покупки (POS терминал)';
COMMENT ON COLUMN companies.default_card_payment_sales_account_id IS 'Плащания с карта при продажби (POS терминал)';
