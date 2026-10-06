-- =====================================================================
-- V15: Add active status column to accounts table
-- Traces to: ACC-010 (Accounting Core)
-- =====================================================================

ALTER TABLE accounts ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
