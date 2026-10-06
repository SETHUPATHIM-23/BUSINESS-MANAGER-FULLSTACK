import React, { useState, useEffect, useRef } from 'react';
import { db } from './db';
import type { Product } from './db';
import type { Invoice, Customer } from './types';
import { api } from '../../utils/api';
import { getStoredCompanyDetails, fetchCompanySettingsFromBackend } from './CompanySettings';
import InvoiceForm from './InvoiceForm';
import PrintInvoice, { type PrintInvoiceHandle } from './PrintInvoice';

const App: React.FC = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [companyDetails, setCompanyDetails] = useState(() => getStoredCompanyDetails());
  const printRef = useRef<PrintInvoiceHandle>(null);

  useEffect(() => {
    fetchCompanySettingsFromBackend().then(loaded => {
      setCompanyDetails(loaded);
    });

    db.init().then(async () => {
        loadCustomers();
        loadProducts();

        // Load tax rates from the backend (Admin Settings → Tax Rates tab)
        try {
          const taxRes = await api.get('/api/lookups/tax-rates');
          const apiTaxRates = (taxRes.data || []).map((t: any) => {
            const rate = Number(t.rate) || 0;
            const isIgst = Boolean(t.isIgst || (t.name && t.name.toUpperCase().includes('IGST')) || (t.label && t.label.toUpperCase().includes('IGST')));
            
            let components: Array<{ name: string; rate: number }> = [];

            if (t.componentsJson) {
              try {
                const parsed = JSON.parse(t.componentsJson);
                if (Array.isArray(parsed) && parsed.length > 0) {
                  components = parsed.map((c: any) => ({ name: String(c.name), rate: Number(c.rate) || 0 }));
                }
              } catch (e) {}
            }

            const cgst = t.cgstRate != null ? Number(t.cgstRate) : 0;
            const sgst = t.sgstRate != null ? Number(t.sgstRate) : 0;
            const igst = t.igstRate != null ? Number(t.igstRate) : 0;

            let cgstRate = cgst;
            let sgstRate = sgst;
            let igstRate = igst;

            if (components.length === 0) {
              if (cgst > 0) components.push({ name: 'CGST', rate: cgst });
              if (sgst > 0) components.push({ name: 'SGST', rate: sgst });
              if (igst > 0) components.push({ name: 'IGST', rate: igst });
              if (components.length === 0 && rate > 0) {
                if (isIgst) {
                  components.push({ name: 'IGST', rate: rate });
                  igstRate = rate;
                } else {
                  components.push({ name: 'CGST', rate: rate / 2 });
                  components.push({ name: 'SGST', rate: rate / 2 });
                  cgstRate = rate / 2;
                  sgstRate = rate / 2;
                }
              }
            }

            return {
              id: String(t.id),
              label: t.name || t.label || `GST ${rate}%`,
              rate: rate,
              cgstRate,
              sgstRate,
              igstRate,
              components
            };
          });

          if (apiTaxRates.length > 0) {
            setCompanyDetails(prev => ({ ...prev, taxRates: apiTaxRates }));
          }
        } catch {
          // backend unavailable — fall back to company details stored settings
        }
    });
  }, []);

  const loadCustomers = async () => {
    const list = await db.getAllCustomers();
    try {
      const apiRes = await api.get('/api/customers', { params: { size: 1000 } });
      const apiList = apiRes.data.content || [];
      const apiMap = new Map<string, any>();
      for (const ac of apiList) {
        if (ac.name) {
          apiMap.set(ac.name.toLowerCase().trim(), ac);
        }
      }

      const combined = list.map(c => {
        const ac = apiMap.get(c.name.toLowerCase().trim());
        if (ac) {
          return {
            ...c,
            address: ac.address || c.address || '',
            gstin: ac.taxId || ac.gstin || c.gstin || '',
            state: ac.state || c.state || 'Tamil Nadu',
            stateCode: ac.stateCode || c.stateCode || '33'
          };
        }
        return c;
      });

      for (const ac of apiList) {
        if (!combined.some(c => c.name.toLowerCase().trim() === ac.name.toLowerCase().trim())) {
          combined.push({
            id: ac.id,
            name: ac.name,
            address: ac.address || '',
            state: ac.state || 'Tamil Nadu',
            stateCode: ac.stateCode || '33',
            gstin: ac.taxId || ac.gstin || ''
          });
        }
      }
      setCustomers(combined);
    } catch (err) {
      setCustomers(list);
    }
  };

  const loadProducts = async () => {
    const list = await db.getAllProducts();
    try {
      const apiRes = await api.get('/api/products', { params: { size: 1000 } });
      const apiList = apiRes.data.content || [];
      const combined = [...list];
      for (const ap of apiList) {
        if (!combined.some(p => p.name.toLowerCase() === ap.name.toLowerCase())) {
          combined.push({
            id: ap.id,
            name: ap.name,
            hsnCode: ap.hsnCode || ap.hsn || '282810',
            baseSellingPrice: ap.baseSellingPrice || 0,
            rates: [ap.baseSellingPrice || 0]
          });
        }
      }
      setProducts(combined);
    } catch (err) {
      setProducts(list);
    }
  };

  const handleInvoiceCreated = (newInvoice: Invoice) => {
    setSelectedInvoice(newInvoice);
  };

  return (
    <div data-theme="light" className="max-w-7xl mx-auto px-4 py-6" style={{ minHeight: '100vh', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)', borderRadius: 'var(--border-radius-lg)' }}>
      <header className="flex flex-col md:flex-row justify-between items-center mb-8 border-b-2 border-blue-600 pb-4 no-print gap-4">
        <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xl shadow-lg">S</div>
            <div>
                <h1 className="text-2xl font-black text-blue-900 tracking-tight uppercase leading-none">{companyDetails.name}</h1>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mt-1">Official GST Billing Portal</p>
            </div>
        </div>

      </header>

      {!selectedInvoice && (
        <div className="no-print space-y-6">
          <InvoiceForm customers={customers} products={products} companyDetails={companyDetails} onInvoiceCreated={handleInvoiceCreated} />
        </div>
      )}

      {selectedInvoice && (
        <div className="invoice-preview-wrapper animate-in fade-in duration-300">
            <div className="flex flex-col items-center mb-4 no-print max-w-lg mx-auto text-center">
                <div className="flex gap-4">
                    <button 
                        onClick={() => setSelectedInvoice(null)}
                        className="bg-slate-700 hover:bg-slate-800 text-white px-6 py-2.5 rounded-lg transition font-bold uppercase text-xs shadow-md"
                    >
                        ← Create New Invoice
                    </button>
                </div>
            </div>
            <PrintInvoice ref={printRef} invoice={selectedInvoice} storageHandle={null} companyDetails={companyDetails} />
        </div>
      )}

    </div>
  );
};

export default App;
