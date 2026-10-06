import type { Customer, Invoice } from './types';

const DB_NAME = 'SenthurChemicalsDB_v3';
const DB_VERSION = 3;

export function incrementInvoiceNumber(str: string): string {
  if (!str || !str.trim()) return '1001';
  const trimmed = str.trim();
  const match = trimmed.match(/^(.*?)(\d+)$/);
  if (!match) {
    return `${trimmed}-0001`;
  }
  const prefix = match[1];
  const digitsStr = match[2];
  try {
    const nextNum = (BigInt(digitsStr) + 1n).toString();
    const paddedDigits = nextNum.length < digitsStr.length ? nextNum.padStart(digitsStr.length, '0') : nextNum;
    return `${prefix}${paddedDigits}`;
  } catch {
    return `${trimmed}-1`;
  }
}

export interface Product {
  id?: number;
  name: string;
  hsn?: string;
  hsnCode?: string;
  rates?: number[];
  baseSellingPrice?: number;
  sku?: string;
}

export class Database {
  private db: IDBDatabase | null = null;
  private initPromise: Promise<void> | null = null;

  async init(): Promise<void> {
    if (this.initPromise) return this.initPromise;

    this.initPromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onerror = () => {
        this.initPromise = null;
        reject(request.error);
      };
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        
        if (!db.objectStoreNames.contains('customers')) {
          db.createObjectStore('customers', { keyPath: 'id', autoIncrement: true });
        }
        
        if (!db.objectStoreNames.contains('invoices')) {
          const invoiceStore = db.createObjectStore('invoices', { keyPath: 'id', autoIncrement: true });
          invoiceStore.createIndex('invoiceNo', 'invoiceNo', { unique: false });
        }

        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings');
        }

        if (!db.objectStoreNames.contains('products')) {
          db.createObjectStore('products', { keyPath: 'id', autoIncrement: true });
        }
      };
    });

    return this.initPromise;
  }

  async addCustomer(customer: Customer): Promise<number> {
    return this.perform('customers', 'readwrite', (store) => store.add(customer));
  }

  async deleteCustomer(id: number): Promise<void> {
    return this.perform('customers', 'readwrite', (store) => store.delete(id));
  }

  async getAllCustomers(): Promise<Customer[]> {
    return this.perform('customers', 'readonly', (store) => store.getAll());
  }

  async addInvoice(invoice: Invoice): Promise<number> {
    try {
      const invCopy = { ...invoice };
      delete invCopy.id;
      return await this.perform('invoices', 'readwrite', (store) => store.add(invCopy));
    } catch (err) {
      console.warn('IndexedDB invoice storage fallback:', err);
      return Date.now();
    }
  }

  async deleteInvoice(id: number | string): Promise<void> {
    return this.perform('invoices', 'readwrite', (store) => store.delete(id));
  }

  async getAllInvoices(): Promise<Invoice[]> {
    return this.perform('invoices', 'readonly', (store) => store.getAll());
  }

  async saveSettings(settings: any): Promise<void> {
    return this.perform('settings', 'readwrite', (store) => store.put(settings, 'company_info'));
  }

  async getSettings(): Promise<any | null> {
    return this.perform('settings', 'readonly', (store) => store.get('company_info'));
  }

  async addProduct(product: Product): Promise<number> {
    return this.perform('products', 'readwrite', (store) => store.add(product));
  }

  async getAllProducts(): Promise<Product[]> {
    return this.perform('products', 'readonly', (store) => store.getAll());
  }

  async deleteProduct(id: number): Promise<void> {
    return this.perform('products', 'readwrite', (store) => store.delete(id));
  }

  async getNextInvoiceNumber(): Promise<string> {
    const saved = localStorage.getItem('last_billed_invoice_number');
    if (saved && saved.trim()) {
      return incrementInvoiceNumber(saved.trim());
    }

    const invoices = await this.getAllInvoices();
    if (invoices.length === 0) return '1001';
    
    for (let i = invoices.length - 1; i >= 0; i--) {
      const no = invoices[i]?.invoiceNo;
      if (no && no.trim()) {
        return incrementInvoiceNumber(no.trim());
      }
    }
    
    return '1001';
  }

  private async perform<T>(storeName: string, mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest): Promise<T> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      try {
        const tx = this.db!.transaction(storeName, mode);
        const store = tx.objectStore(storeName);
        const request = action(store);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
      } catch (err) {
        reject(err);
      }
    });
  }
}

export const db = new Database();
