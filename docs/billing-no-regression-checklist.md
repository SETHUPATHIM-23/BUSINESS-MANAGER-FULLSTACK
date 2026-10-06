# Billing Module No-Regression Contract

This checklist serves as the benchmark for Phase D (Final Integration and Deletion). As we integrate the `billingex` features (tax inclusive calculations, offline UI styling, print previews) into the enterprise backend, **all of the following existing features must remain fully functional without regression.**

## 1. Routes & Access Control
- [ ] **`/billing` Route:** The primary invoicing dashboard must remain accessible.
- [ ] **`/pos` Route:** The POS-style quick billing terminal must remain accessible and functional.
- [ ] **Role-Based Access Control (RBAC):** 
  - `BILLING_READ` for viewing the list.
  - `BILLING_WRITE` for creating, editing, and recording payments.
  - `BILLING_DELETE` for deleting drafts.
  - `BILLING_POST` for posting invoices to accounting/inventory.

## 2. Invoice List & Dashboard Features
- [ ] **Server-Side Pagination:** The data table must successfully paginate against `/api/billings`.
- [ ] **Search:** Text search by Invoice Number or Customer Name must work.
- [ ] **Date Filtering:** Filtering by `dateFrom` and `dateTo` must work.
- [ ] **Status Filtering:** Filtering by `DRAFT`, `SENT`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `VOIDED`, `CANCELLED` must work.
- [ ] **Sorting:** Clicking column headers (Invoice #, Date, Total, Balance) must trigger server-side sorting.

## 3. Invoice Lifecycle (Create / Edit / Delete)
- [ ] **Creation (`DRAFT`):** Creating a new invoice via the form modal must successfully hit `POST /api/billings` and default to `DRAFT` status.
- [ ] **Client-Side Validation:** Must enforce customer selection, at least one line item, and quantity > 0 before submission.
- [ ] **Editing:** Draft invoices can be re-opened in the form modal, populated via `GET /api/billings/{id}`, and updated via `PUT`.
- [ ] **Permanent Deletion:** Only `DRAFT` invoices can be permanently deleted via the Trash icon.

## 4. Financial Actions (Posting & Payments)
- [ ] **Posting Invoices:** Clicking the "Send/Post" icon on a draft invoice must trigger `POST /api/billings/{id}/post`, locking the invoice and integrating with Inventory/Accounting.
- [ ] **Payment Collection:** Clicking the Dollar icon on an active invoice must prompt for an amount and successfully record the payment via `POST /api/billings/{id}/payments`.
- [ ] **Payment Validation:** The system must reject payment inputs that exceed the `amountOutstanding`.

## 5. Reversals & Returns
- [ ] **Voiding/Cancelling:** Clicking the "X" icon on a posted/overdue invoice must trigger `POST /api/billings/{id}/cancel` to reverse the financial and stock impact.

## 6. External Integrations
- [ ] **Network Printing:** Clicking the Print icon must successfully prompt for a network printer name and queue the job via `POST /api/billings/{id}/print`.
