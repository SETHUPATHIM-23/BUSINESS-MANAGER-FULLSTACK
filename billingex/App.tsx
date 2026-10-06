
import React, { useState, useEffect, useRef, useMemo } from 'react';
import { db, Product } from './db';
import { Invoice, Customer, COMPANY_DETAILS } from './types';
import CustomerManager from './components/CustomerManager';
import CompanySettings from './components/CompanySettings';
import ProductManager from './components/ProductManager';
import InvoiceForm from './components/InvoiceForm';
import PrintInvoice, { PrintInvoiceHandle } from './components/PrintInvoice';
import { exportInvoicesToCSV, formatTime12Hour } from './utils';

const App: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [view, setView] = useState<'create' | 'list'>('create');
  const [storageHandle, setStorageHandle] = useState<any>(null);
  const [companyDetails, setCompanyDetails] = useState(COMPANY_DETAILS);
  const printRef = useRef<PrintInvoiceHandle>(null);

  useEffect(() => {
    db.init().then(async () => {
        loadInvoices();
        loadCustomers();
        loadProducts();
        const savedSettings = await db.getSettings();
        if (savedSettings) {
          setCompanyDetails(savedSettings);
        }
    });
  }, []);

  const loadInvoices = async () => {
    const list = await db.getAllInvoices();
    setInvoices(list.reverse()); 
  };

  const loadCustomers = async () => {
    const list = await db.getAllCustomers();
    setCustomers(list);
  };

  const loadProducts = async () => {
    const list = await db.getAllProducts();
    setProducts(list);
  };

  const handleInvoiceCreated = (newInvoice: Invoice) => {
    setSelectedInvoice(newInvoice);
    loadInvoices();
  };

  const handlePrint = async () => {
    if (printRef.current) {
      await printRef.current.printToSystem();
    }
  };

  const selectStorageFolder = async () => {
    try {
      // @ts-ignore
      const handle = await window.showDirectoryPicker();
      setStorageHandle(handle);
    } catch (err) {
      console.warn("Folder selection cancelled or not supported.");
    }
  };

  // Memoize large lists for performance
  const invoiceRows = useMemo(() => {
    return invoices.map(inv => (
      <tr key={inv.id} className="hover:bg-blue-50 transition border-b border-gray-100 last:border-0">
          <td className="p-4 text-sm whitespace-nowrap">
            <div>{new Date(inv.invoiceDate).toLocaleDateString()}</div>
            {inv.invoiceTime && <div className="text-[11px] font-bold text-gray-500">{formatTime12Hour(inv.invoiceTime)}</div>}
          </td>
          <td className="p-4 font-bold text-blue-700">{inv.invoiceNo}</td>
          <td className="p-4 text-sm font-medium uppercase break-words">{inv.customerName}</td>
          <td className="p-4 text-right font-bold text-gray-900">₹ {inv.grandTotal.toFixed(2)}</td>
          <td className="p-4 text-center">
              <button 
                  onClick={() => setSelectedInvoice(inv)}
                  className="bg-blue-100 text-blue-700 hover:bg-blue-600 hover:text-white px-3 py-1 rounded transition font-bold text-xs uppercase"
              >
                  Reprint
              </button>
          </td>
      </tr>
    ));
  }, [invoices]);

  return (
    <div className="max-w-7xl mx-auto px-4 py-6">
      <header className="flex flex-col md:flex-row justify-between items-center mb-8 border-b-2 border-blue-600 pb-4 no-print gap-4">
        <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xl shadow-lg">S</div>
            <div>
                <h1 className="text-2xl font-black text-blue-900 tracking-tight uppercase leading-none">{companyDetails.name}</h1>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mt-1">Official GST Billing Portal</p>
            </div>
        </div>

        <div className="flex flex-col items-end gap-2">
          <div className="flex gap-2">
              <button 
                  onClick={() => { setView('create'); setSelectedInvoice(null); }} 
                  className={`px-4 py-2 rounded-md font-bold transition shadow-sm ${view === 'create' ? 'bg-blue-600 text-white' : 'bg-white text-blue-600 hover:bg-blue-50 border border-blue-200'}`}
              >
                  New Bill
              </button>
              <button 
                  onClick={() => { setView('list'); setSelectedInvoice(null); }} 
                  className={`px-4 py-2 rounded-md font-bold transition shadow-sm ${view === 'list' ? 'bg-blue-600 text-white' : 'bg-white text-blue-600 hover:bg-blue-50 border border-blue-200'}`}
              >
                  Records
              </button>
          </div>
          
          <button 
            onClick={selectStorageFolder}
            className={`flex items-center gap-2 px-3 py-1 rounded text-[10px] font-black uppercase transition border ${storageHandle ? 'bg-green-50 text-green-700 border-green-200' : 'bg-orange-50 text-orange-700 border-orange-200'}`}
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            {storageHandle ? `Saving to: ${storageHandle.name}` : 'Link Local Folder (Auto-Save)'}
          </button>
        </div>
      </header>

      {view === 'create' && !selectedInvoice && (
        <div className="no-print space-y-6">
          <CompanySettings currentSettings={companyDetails} onSettingsChanged={setCompanyDetails} />
          <CustomerManager customers={customers} onCustomerChanged={loadCustomers} />
          <ProductManager products={products} onProductsChanged={loadProducts} />
          <InvoiceForm customers={customers} products={products} companyDetails={companyDetails} onInvoiceCreated={handleInvoiceCreated} />
        </div>
      )}

      {selectedInvoice && (
        <div className="invoice-preview-wrapper animate-in fade-in duration-300">
            <div className="flex flex-col items-center mb-6 no-print max-w-lg mx-auto text-center">
                <div className="flex gap-4 mb-4">
                    <button 
                        onClick={handlePrint}
                        className="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700 transition font-bold shadow-xl flex items-center gap-2 text-lg uppercase tracking-tighter active:scale-95"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 00-2 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
                        </svg>
                        Confirm & Send to Printer
                    </button>
                    <button 
                        onClick={() => setSelectedInvoice(null)}
                        className="bg-white text-gray-700 px-6 py-3 rounded-lg hover:bg-gray-100 transition font-bold border border-gray-300 uppercase text-xs"
                    >
                        Cancel
                    </button>
                </div>
            </div>
            <PrintInvoice ref={printRef} invoice={selectedInvoice} storageHandle={storageHandle} companyDetails={companyDetails} />
        </div>
      )}

      {view === 'list' && !selectedInvoice && (
        <div className="bg-white rounded-lg shadow-sm border overflow-hidden no-print">
            <div className="p-4 bg-white border-b flex justify-between items-center">
                <div>
                  <h2 className="font-bold text-lg text-gray-700 uppercase tracking-tighter">Local Invoice Registry</h2>
                  <span className="text-sm font-medium text-gray-500 bg-gray-100 px-2 py-1 rounded">{invoices.length} Total Bills</span>
                </div>
                <button 
                  onClick={() => exportInvoicesToCSV(invoices)}
                  className="bg-green-600 text-white px-4 py-2 rounded-md font-bold text-sm hover:bg-green-700 transition shadow flex items-center gap-2"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                  </svg>
                  Export CSV
                </button>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead className="bg-white text-xs font-bold uppercase text-gray-600 border-b">
                        <tr>
                            <th className="p-4">Date</th>
                            <th className="p-4">Invoice #</th>
                            <th className="p-4">Customer</th>
                            <th className="p-4 text-right">Amount (Inc. GST)</th>
                            <th className="p-4 text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {invoiceRows}
                        {invoices.length === 0 && (
                          <tr>
                            <td colSpan={5} className="p-10 text-center text-gray-400 italic">No records found.</td>
                          </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
      )}
    </div>
  );
};

export default App;
