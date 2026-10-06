ALTER TABLE invoices ADD COLUMN igst DECIMAL(15,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN cgst_rate DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN sgst_rate DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN igst_rate DECIMAL(5,2) DEFAULT 0.00;

ALTER TABLE tax_rates ADD COLUMN cgst_rate DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE tax_rates ADD COLUMN sgst_rate DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE tax_rates ADD COLUMN igst_rate DECIMAL(5,2) DEFAULT 0.00;

UPDATE tax_rates SET cgst_rate = rate / 2, sgst_rate = rate / 2, igst_rate = 0 WHERE rate IS NOT NULL;
