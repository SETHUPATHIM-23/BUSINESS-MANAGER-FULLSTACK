# Stack & Structure Audit: `billingex`

## 1. Overview
The `billingex` folder contains a standalone, client-side React billing and invoice management application. It is configured to run as a web application and also includes configuration to be compiled into a desktop application using Tauri.

**Backend Note:** **The application has no backend of its own.** All data is stored and managed client-side using the browser's native `IndexedDB` (configured in `db.ts`). It does not rely on a traditional database (like PostgreSQL or MongoDB) or a mock backend server, meaning all state is persistent only on the local machine/browser.

## 2. Language & Frameworks
- **Frontend Framework:** React 19 (using `react` and `react-dom`).
- **Language:** TypeScript.
- **Styling:** Tailwind CSS (inferred from utility classes like `max-w-7xl`, `flex-col`, `bg-blue-600` used heavily in components).
- **Desktop Wrapper:** Tauri (Rust-based), allowing the web app to be built as a native desktop executable.

## 3. Build Tooling & Package Manager
- **Build Tooling:** Vite (`vite` v6, `@vitejs/plugin-react`). It handles the bundling, development server, and environment variable injection.
- **Package Manager:** Designed for `npm`/`yarn` (based on standard `package.json` configurations).

## 4. Entry Points
- **Web Entry:** `index.html` (Vite's standard entry file).
- **React Entry:** `index.tsx`, which mounts the React tree to the DOM.
- **Main Application Component:** `App.tsx`, which handles the primary routing (via state), layout, and orchestration of child components.
- **Tauri Entry:** `src-tauri/Cargo.toml` and `src-tauri/src/main.rs` (inferred from Tauri structure) serve as the native desktop shell entry point.

## 5. Folder Structure
- **Root Directory:** Contains all configuration and core logic files.
  - `package.json`, `tsconfig.json`, `vite.config.ts`: Project build and dependency configs.
  - `.env.local`: Local environment variables (notably for API keys).
  - `db.ts`: IndexedDB initialization and wrapper functions for local data persistence.
  - `utils.ts`: Helper functions (e.g., CSV export, time formatting).
  - `types.ts`: TypeScript interfaces and type definitions (`Customer`, `Invoice`, etc.).
- **`components/`**: Contains modular React components for the UI.
  - `CompanySettings.tsx`: Form for updating company details.
  - `CustomerManager.tsx`: UI for adding/managing customers.
  - `InvoiceForm.tsx`: The main form for creating new invoices.
  - `ProductManager.tsx`: UI for adding/managing product catalogs.
  - `PrintInvoice.tsx`: The template component used for rendering and printing invoices.
- **`src-tauri/`**: Contains the Tauri project structure (`Cargo.toml`, `tauri.conf.json`, `src/`) for building the desktop application.

## 6. External Services & APIs
- **Local Storage:** Relies exclusively on **IndexedDB** (`db.ts`) for persisting customers, products, invoices, and settings locally on the user's machine.
- **File System API:** Uses the native browser File System Access API (`window.showDirectoryPicker()`) to request a local folder for auto-saving invoices or exported data.
- **PDF Generation:** Uses `jspdf` and `html2canvas` to render DOM elements into printable PDF files.
- **External API:** `vite.config.ts` exposes `process.env.GEMINI_API_KEY` to the application, indicating it likely integrates with the **Google Gemini API** (possibly for data extraction or generative features within the components). No other external BaaS (like Firebase or Supabase) is used.

## 7. Requirement Coverage Map Against Master SRS (BILL-010 to BILL-110)

| Feature / Requirement | Status | Notes |
| :--- | :--- | :--- |
| **Invoice Creation** (BILL-020 Multi-line invoices) | **Present** | Forms support adding multiple products (`InvoiceForm.tsx`) with calculated amounts. |
| **Line Calculation** (BILL-020) | **Present** | Real-time calculations for quantity * rate across lines. |
| **Tax** | **Present** | Implements tax calculations based on selected Bill Type ('tax_exclusive', 'tax_inclusive'). Correctly calculates CGST and SGST and handles round off. |
| **Discounts** (BILL-030 Pricing/Tiers) | **Missing** | No discount logic, customer price tiers, or invoice-level discounts are implemented. |
| **Sequential Numbering** (BILL-010 Gap-free) | **Present** | Generates sequential strings (e.g., `SC-2026-0001`) locally. |
| **Credit-hold/Credit-limit Checks** (CUST-030/060) | **Missing** | No validations exist to prevent billing based on credit limits or hold statuses. |
| **Stock-sufficiency Checks** (BILL-040 Real-time deduction) | **Missing** | No inventory quantities are stored or validated against; stock is not deducted. |
| **Payment Collection** (BILL-070 Payment receipts) | **Missing** | No screens or fields to record payments, allocations, or track account balances. |
| **Sales Returns/Credit Notes** (BILL-090 Returns) | **Missing** | Only standard sales invoices are supported; no returns flow exists. |
| **Draft Cancellation vs Posted Reversal** (BILL-080 Immutability) | **Missing** | No posted/draft states or immutability rules exist. Invoices are just saved to IndexedDB. |
| **Print Routing** (BILL-060 Print queue) | **Present** | Supports printing via browser API and clean client-side/server-side PDF generation. |

### Features present in `billingex` but missing from the Master SRS:
- **Local Auto-Save Directory:** Integrates the browser's File System Access API (`window.showDirectoryPicker()`) to automatically store local backup copies of the generated PDFs.
- **Offline / Local Execution:** Designed to run 100% locally and offline without a central database.
- **Specific Bill Types (Tax Inclusive/Exclusive/Challan):** The UI provides toggles to switch calculation models seamlessly depending on the compliance scenario (Delivery Challans skip tax).
- **Vehicle Selection Input:** Built-in form for recording delivery vehicles (`vehicleNo`) directly on the invoice, rather than treating trucks as a separate delivery assignment module.

## 8. Data Model Diff
Below is a field-by-field mapping between `billingex`'s local data shapes and the existing enterprise backend entities (`Invoice`, `InvoiceLine`, `Customer`, `Product`).

### Invoice Mapping
| `billingex` Invoice Field | Backend `Invoice` Field | Notes |
| :--- | :--- | :--- |
| `id` | `id` (BaseEntity) | Exact match. |
| `invoiceNo` | `invoiceNumber` | Rename. |
| `invoiceDate` | `invoiceDate` | Exact match. |
| `invoiceTime` | - | New field we need to add, or we can change `invoiceDate` to `LocalDateTime`. |
| `customerId` | `customer` | Exact match (relationship mapping). |
| `customerName` | - | Field we should discard as redundant (accessible via `customer.name`). |
| `customerAddress` | - | Field we should discard as redundant (accessible via `customer.address`). |
| `customerGSTIN` | - | Field we should discard as redundant (accessible via `customer.taxId`). |
| `customerState` | - | Field we should discard as redundant (accessible if we add state to `Customer`). |
| `customerStateCode` | - | Field we should discard as redundant (accessible if we add stateCode to `Customer`). |
| `vehicleNo` | - | New field we need to add to `Invoice`, unless we rely strictly on the TRUCK module. |
| `billType` | - | New field we need to add (Enum for tax_exclusive, tax_inclusive, delivery_challan). |
| `items` | `lines` | Rename (list of InvoiceLines). |
| `subtotal` | `subtotal` | Exact match. |
| `cgst` | - | New field we need to add (or split existing `taxTotal` into components). |
| `sgst` | - | New field we need to add. |
| `roundOff` | - | New field we need to add to capture rounding discrepancies. |
| `grandTotal` | `grandTotal` | Exact match. |
| `totalInWords` | - | Field we should discard as redundant (should be dynamically computed by frontend/PDF renderer). |
| `notes` | `notes` | Exact match. |
| `gstPercentage` | - | New field we need to add (represents the document-level tax rate applied). |

### InvoiceLine (billingex `InvoiceItem`) Mapping
| `billingex` InvoiceItem Field | Backend `InvoiceLine` Field | Notes |
| :--- | :--- | :--- |
| `id` | `id` (BaseEntity) | Exact match. |
| - | `product` | New field we need to add to `billingex` (pass the actual `productId` rather than just the name). |
| `productName` | - | Field we should discard as redundant (accessible via `product.name`). |
| `hsnCode` | - | Field we should discard as redundant (accessible via `product`). |
| `quantity` | `quantity` | Exact match. |
| `rate` | `unitPrice` | Rename. |
| `amount` | `lineTotal` | Rename. |

### Customer Mapping
| `billingex` Customer Field | Backend `Customer` Field | Notes |
| :--- | :--- | :--- |
| `id` | `id` (BaseEntity) | Exact match. |
| `name` | `name` | Exact match. |
| `address` | `address` | Exact match. |
| `state` | - | New field we need to add to the backend `Customer` entity. |
| `stateCode` | - | New field we need to add to the backend `Customer` entity. |
| `gstin` | `taxId` | Rename. |

### Product Mapping
| `billingex` Product Field | Backend `Product` Field | Notes |
| :--- | :--- | :--- |
| `id` | `id` (BaseEntity) | Exact match. |
| `name` | `name` | Exact match. |
| `hsn` | `sku` | Rename (or add `hsnCode` as a separate field, which is common in Indian billing). |
| `rates` | `baseSellingPrice` | `billingex` uses a multiple-rate array; we need to decide if we add a `rates` JSON/array field or enforce single `baseSellingPrice` with Customer Price Tiers. |

## 9. Business Logic Inventory (Phase B Checklist)
This is an exhaustive list of the specific business rules, calculations, and behaviors currently embedded in the `billingex` frontend. When migrating/integrating to the enterprise backend, each of these must be ported to the backend services or explicitly documented if dropped/refactored.

1. **Invoice Number Sequencing (Gap-Free Generation)**
   - **Source:** `billingex/db.ts` -> `Database.getNextInvoiceNumber()`
   - **Logic:** Reads all local invoices, sorts them, and checks the last invoice number. If it matches the current year, it increments the 4-digit sequence (e.g., `SC-2026-0012`). If the year changed, it resets the sequence to `0001`.

2. **Line Item Amount Calculation (Tax-Aware)**
   - **Source:** `billingex/components/InvoiceForm.tsx` -> `getEffectiveItems()` and `handleAddItem()`
   - **Logic:** For `tax_exclusive` and `delivery_challan`, `amount = round(qty * rate)`. For `tax_inclusive`, it reverse-calculates the base value before tax by dividing `(qty * rate) / (1 + (gstPercentage / 100))`. Uses custom `roundTo2()` for precision.

3. **Document-Level Tax and Rounding Calculation**
   - **Source:** `billingex/components/InvoiceForm.tsx` -> `getCalculations()`
   - **Logic:** 
     - Calculates `CGST` and `SGST` as half of the document's total GST percentage (default 18%).
     - Calculates a strictly rounded `grandTotal`.
     - Calculates `roundOff` as the exact mathematical difference between the rounded `grandTotal` and the unrounded sum of `subtotal + cgst + sgst`.
     - Completely zeroes out taxes if the bill type is `delivery_challan`.

4. **Indian Number System Conversion (Amount to Words)**
   - **Source:** `billingex/utils.ts` -> `numberToWords()`
   - **Logic:** Converts the numeric `grandTotal` into an English string formatted according to the Indian numbering system (Crores, Lakhs, Thousands, Rupees, Paise).

5. **Multi-Rate Default Selection**
   - **Source:** `billingex/components/InvoiceForm.tsx` -> `useEffect` (on `selectedProductId` change)
   - **Logic:** Automatically sets the item's billing rate to the first element in the product's `rates` array when a new product is selected.

6. **Default Vehicle Assignment**
   - **Source:** `billingex/components/InvoiceForm.tsx` -> `useEffect` (on `companyDetails` change)
   - **Logic:** Automatically assigns the first configured company vehicle to the invoice if one is not already manually selected.

7. **Dual-Page PDF Generation (Original / Duplicate)**
   - **Source:** `billingex/components/PrintInvoice.tsx` -> `generateDualPagePDF()`
   - **Logic:** Uses `html2canvas` and `jsPDF` to stamp out a 2-page document. Page 1 is explicitly watermarked with `(ORIGINAL FOR RECIPIENT)` and Page 2 with `(DUPLICATE FOR TRANSPORTER)`. It also forcibly hides UI buttons (`#copy-label-slot`) during clone.

8. **Local Hierarchical Archiving (File System Access)**
   - **Source:** `billingex/utils.ts` -> `getStructuredFolder()` and `saveFileToFolder()`
   - **Logic:** Organizes saved invoice PDFs and CSVs into local directories partitioned by Year and Month (e.g., `2026/August/`) utilizing the native browser File System Access API.

9. **Invisible Iframe Print Spooling**
   - **Source:** `billingex/components/PrintInvoice.tsx` -> `handlePrintDirect()`
   - **Logic:** To avoid popups, it injects an invisible 1x1 pixel iframe containing the generated PDF blob directly into the DOM, waits 1.2s for the browser to spool, and natively triggers `contentWindow.print()`.

## 10. UI Component Inventory & State Architecture

### Component Tree
- `App` (`App.tsx` - Root Orchestrator)
  - `CompanySettings` (`CompanySettings.tsx`)
  - `CustomerManager` (`CustomerManager.tsx`)
  - `ProductManager` (`ProductManager.tsx`)
  - `InvoiceForm` (`InvoiceForm.tsx`)
  - `PrintInvoice` (`PrintInvoice.tsx`)

### Styling Approach
- **CSS Framework:** Tailwind CSS is used exclusively for structural and aesthetic styling via utility classes (e.g., `bg-indigo-600`, `flex-col`, `rounded-lg`). There are no heavy third-party component libraries (like MUI, Antd, or Shadcn).
- **Inline Styles:** Specifically used inside `PrintInvoice.tsx` to enforce strict, unyielding A4 page dimensions (`210mm` x `297mm`) and absolute print positioning, safely isolating the print template from global responsive CSS rules.

### State Management Pattern
- **Top-Down Data Flow:** `App.tsx` serves as the centralized state orchestrator. It uses simple `useState` hooks to hold the master lists of `invoices`, `customers`, `products`, and `companyDetails`. 
- **Hydration:** `App.tsx` queries the local database on mount (`useEffect`) and passes the data down to child components as React props.
- **Child Mutations:** When a child component needs to alter data (e.g., `CustomerManager` adding a customer), it executes the mutation directly against the database and then fires a callback prop (e.g., `onCustomerChanged()`) to tell `App.tsx` to re-fetch the entire updated list from the database.

### Portability & Coupling Analysis
A critical factor for Phase B integration is how tightly these UI components are coupled to the local IndexedDB database (`db.ts`).

- **Tightly Coupled (Requires Refactoring before porting):** 
  - `CustomerManager`, `ProductManager`, `CompanySettings`, `InvoiceForm`. 
  - **Why:** These components directly import and invoke `db.ts` methods (like `db.addCustomer()` or `db.getNextInvoiceNumber()`). To integrate them into the enterprise frontend, all `db.ts` references must be ripped out and replaced with standard Axios API calls to our Spring Boot backend.
  
- **Self-Contained & Highly Portable:**
  - `PrintInvoice`
  - **Why:** This component is completely decoupled from the data layer. It accepts all required data strictly through React props (`invoice`, `companyDetails`) and relies solely on pure helper functions from `utils.ts`. It can be lifted and shifted into the enterprise app with almost zero modifications.
