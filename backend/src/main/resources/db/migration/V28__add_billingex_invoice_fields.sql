-- Add billingex specific fields to Invoices
ALTER TABLE invoices ADD COLUMN invoice_time TIME;
ALTER TABLE invoices ADD COLUMN vehicle_no VARCHAR(100);
ALTER TABLE invoices ADD COLUMN bill_type VARCHAR(30);
ALTER TABLE invoices ADD COLUMN cgst DECIMAL(15, 2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN sgst DECIMAL(15, 2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN round_off DECIMAL(15, 2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN gst_percentage DECIMAL(5, 2) DEFAULT 18.00;

-- Add billingex specific fields to Customers
ALTER TABLE customers ADD COLUMN state VARCHAR(100);
ALTER TABLE customers ADD COLUMN state_code VARCHAR(10);

-- Add billingex specific fields to Products
ALTER TABLE products ADD COLUMN hsn_code VARCHAR(50);
