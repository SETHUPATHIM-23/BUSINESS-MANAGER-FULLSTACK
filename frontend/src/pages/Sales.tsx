import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { toastEvents } from '../utils/toast';
import { useAuth } from '../context/AuthContext';
import { 
  Plus, Search, Trash2, Printer, ChevronLeft, ChevronRight, 
  ArrowUpDown, RefreshCw, X, Receipt
} from 'lucide-react';
import { db } from '../components/billing/db';
import type { Invoice as StorageInvoice } from '../components/billing/types';
import PrintInvoice, { type PrintInvoiceHandle } from '../components/billing/PrintInvoice';
import { getStoredCompanyDetails, fetchCompanySettingsFromBackend } from '../components/billing/CompanySettings';

interface SalesSummary {
  id: number | string;
  invoiceNumber: string;
  customerName: string;
  invoiceDate: string;
  grandTotal: number;
  amountPaid?: number;
  status?: string;
  vehicleNo?: string;
  billType?: string;
  isLocalOnly?: boolean;
  rawInvoice?: StorageInvoice;
}

export const Sales: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('BILLING_WRITE');

  const [sales, setSales] = useState<SalesSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [, setTotalElements] = useState(0);

  const [sortBy, setSortBy] = useState('invoiceDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Print modal state
  const [printModalOpen, setPrintModalOpen] = useState(false);
  const [printingInvoice, setPrintingInvoice] = useState<StorageInvoice | null>(null);
  const printRef = useRef<PrintInvoiceHandle>(null);

  // Company Details state
  const [companyDetails, setCompanyDetails] = useState(() => getStoredCompanyDetails());

  useEffect(() => {
    fetchCompanySettingsFromBackend().then(loaded => {
      setCompanyDetails(loaded);
    });
  }, []);

  const fetchSales = async () => {
    try {
      setLoading(true);
      
      // Load IndexedDB local invoices first
      await db.init();
      const localInvoices = await db.getAllInvoices();
      
      const mappedLocal: SalesSummary[] = localInvoices.map(inv => ({
        id: inv.id || inv.invoiceNo,
        invoiceNumber: inv.invoiceNo,
        customerName: inv.customerName,
        invoiceDate: inv.invoiceDate || (inv as any).date || new Date().toISOString().split('T')[0],
        grandTotal: inv.grandTotal || (inv as any).totalAmount || (inv as any).subtotal || 0,
        vehicleNo: inv.vehicleNo,
        billType: inv.billType,
        isLocalOnly: true,
        rawInvoice: inv
      }));

      // Try fetching from backend API
      try {
        const params: Record<string, any> = {
          page,
          size: pageSize,
          sort: `${sortBy},${sortDir}`
        };
        if (search) params.search = search;

        const res = await api.get('/api/billings', { params });
        const apiContent = res.data.content || [];
        
        const mappedApi: SalesSummary[] = apiContent.map((inv: any) => ({
          id: inv.id,
          invoiceNumber: inv.invoiceNumber,
          customerName: inv.customerName || inv.customer?.name || 'Customer',
          invoiceDate: inv.invoiceDate,
          grandTotal: inv.grandTotal,
          amountPaid: inv.amountPaid ?? 0,
          status: inv.status,
          vehicleNo: inv.vehicleNo,
          billType: inv.billType,
          isLocalOnly: false
        }));

        // Merge API + Local (deduplicate by invoiceNumber)
        const combinedMap = new Map<string, SalesSummary>();
        mappedApi.forEach(item => combinedMap.set(item.invoiceNumber, item));
        mappedLocal.forEach(item => {
          if (!combinedMap.has(item.invoiceNumber)) {
            combinedMap.set(item.invoiceNumber, item);
          }
        });

        let combined = Array.from(combinedMap.values());
        if (search) {
          const q = search.toLowerCase();
          combined = combined.filter(s => 
            s.invoiceNumber.toLowerCase().includes(q) || 
            s.customerName.toLowerCase().includes(q)
          );
        }

        setSales(combined);
        setTotalPages(res.data.totalPages || 1);
        setTotalElements(combined.length);
      } catch (apiErr) {
        // Fallback to local IndexedDB only if API fails
        let filteredLocal = mappedLocal;
        if (search) {
          const q = search.toLowerCase();
          filteredLocal = filteredLocal.filter(s => 
            s.invoiceNumber.toLowerCase().includes(q) || 
            s.customerName.toLowerCase().includes(q)
          );
        }

        setSales(filteredLocal);
        setTotalPages(Math.ceil(filteredLocal.length / pageSize) || 1);
        setTotalElements(filteredLocal.length);
      }
    } catch (err) {
      toastEvents.error('Failed to load sales records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSales();
  }, [page, pageSize, sortBy, sortDir, search]);

  const handleSort = (column: string) => {
    if (sortBy === column) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDir('asc');
    }
    setPage(0);
  };

  const handleDelete = async (sale: SalesSummary) => {
    // Block deletion if the invoice has recorded payments
    if (sale.amountPaid && sale.amountPaid > 0) {
      toastEvents.error('Cannot delete an invoice that has recorded payments. Please cancel or void it instead.');
      return;
    }

    const isDraft = !sale.status || sale.status === 'DRAFT';
    const action = isDraft ? 'permanently delete' : 'cancel and remove';
    if (!window.confirm(`Are you sure you want to ${action} sales invoice ${sale.invoiceNumber}?`)) return;

    try {
      if (!sale.isLocalOnly && typeof sale.id === 'number') {
        await api.delete(`/api/billings/${sale.id}`);
      }
      if (sale.rawInvoice?.id) {
        await db.deleteInvoice(sale.rawInvoice.id);
      }
      toastEvents.success('Sales invoice deleted successfully');
      fetchSales();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Delete failed. Please try again.');
    }
  };

  const handleOpenPrintModal = async (sale: SalesSummary) => {
    let inv: StorageInvoice | null = sale.rawInvoice || null;

    if (!inv && typeof sale.id === 'number') {
      try {
        const res = await api.get(`/api/billings/${sale.id}`);
        const apiInv = res.data;
        inv = {
          id: apiInv.id,
          invoiceNo: apiInv.invoiceNumber,
          invoiceDate: apiInv.invoiceDate,
          customerName: apiInv.customerName || apiInv.customer?.name || '',
          customerAddress: apiInv.customerAddress || apiInv.customer?.address || '',
          customerGSTIN: apiInv.customerGSTIN || apiInv.customerTaxId || apiInv.customer?.taxId || apiInv.customer?.gstin || '',
          customerState: apiInv.customerState || apiInv.customer?.state || 'Tamil Nadu',
          customerStateCode: apiInv.customerStateCode || apiInv.customer?.stateCode || '33',
          date: apiInv.invoiceDate,
          time: new Date().toLocaleTimeString(),
          vehicleNo: apiInv.vehicleNo || '',
          billType: apiInv.billType || 'tax_exclusive',
          taxCategory: apiInv.taxCategory || 'GST 18%',
          paymentMethod: apiInv.paymentMethod || 'Cash',
          items: (apiInv.lines || []).map((l: any, idx: number) => ({
            id: String(l.id || idx),
            sNo: idx + 1,
            productName: l.productName || l.product?.name || '',
            dcNo: l.dcNo || l.dc_no || '-',
            hsnCode: l.hsnCode || l.product?.hsnCode || '282810',
            quantity: l.quantity,
            rate: l.unitPrice,
            amount: l.lineTotal
          })),
          subtotal: apiInv.subtotal || 0,
          subTotal: apiInv.subtotal || 0,
          cgst: apiInv.cgstAmount != null ? apiInv.cgstAmount : 0,
          cgstAmount: apiInv.cgstAmount != null ? apiInv.cgstAmount : 0,
          sgst: apiInv.sgstAmount != null ? apiInv.sgstAmount : 0,
          sgstAmount: apiInv.sgstAmount != null ? apiInv.sgstAmount : 0,
          igst: apiInv.igstAmount != null ? apiInv.igstAmount : 0,
          igstAmount: apiInv.igstAmount != null ? apiInv.igstAmount : 0,
          grandTotal: apiInv.grandTotal || 0,
          totalAmount: apiInv.grandTotal || 0,
          roundOff: apiInv.roundOff || 0,
          totalInWords: apiInv.totalInWords || '',
          notes: apiInv.notes || '',
          taxLines: apiInv.taxLines || apiInv.tax_lines || []
        };
      } catch (err) {
        toastEvents.error('Failed to load invoice details');
        return;
      }
    }

    if (inv) {
      setPrintingInvoice(inv);
      setPrintModalOpen(true);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(val || 0);
  };

  return (
    <div className="w-full space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-blue-600/10 text-blue-600 flex items-center justify-center font-bold">
            <Receipt size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-gray-900">Billed Sales</h1>
            <p className="text-sm text-gray-500">View and print all customer sales bills</p>
          </div>
        </div>

        {hasWriteAccess && (
          <button
            onClick={() => navigate('/billing')}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md hover:shadow-lg transition-all active:scale-95"
          >
            <Plus size={18} />
            <span>New Sales Bill</span>
          </button>
        )}
      </div>

      {/* Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative flex-1 w-full">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search by invoice number or customer name..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full pl-10 pr-4 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
          />
        </div>

        <button
          onClick={fetchSales}
          title="Refresh list"
          className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 text-gray-600 transition"
        >
          <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
        </button>
      </div>

      {/* Sales Data Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 font-semibold uppercase text-xs">
                <th 
                  className="py-3.5 px-4 cursor-pointer hover:text-blue-600 transition"
                  onClick={() => handleSort('invoiceNumber')}
                >
                  <div className="flex items-center gap-1.5">
                    <span>Invoice Number</span>
                    <ArrowUpDown size={14} />
                  </div>
                </th>
                <th 
                  className="py-3.5 px-4 cursor-pointer hover:text-blue-600 transition"
                  onClick={() => handleSort('customerName')}
                >
                  <div className="flex items-center gap-1.5">
                    <span>Customer</span>
                    <ArrowUpDown size={14} />
                  </div>
                </th>
                <th 
                  className="py-3.5 px-4 cursor-pointer hover:text-blue-600 transition"
                  onClick={() => handleSort('invoiceDate')}
                >
                  <div className="flex items-center gap-1.5">
                    <span>Date</span>
                    <ArrowUpDown size={14} />
                  </div>
                </th>
                <th className="py-3.5 px-4">
                  <div className="flex items-center gap-1.5">
                    <span>Vehicle No</span>
                  </div>
                </th>
                <th 
                  className="py-3.5 px-4 cursor-pointer hover:text-blue-600 transition text-right"
                  onClick={() => handleSort('grandTotal')}
                >
                  <div className="flex items-center justify-end gap-1.5">
                    <span>Total Amount</span>
                    <ArrowUpDown size={14} />
                  </div>
                </th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-gray-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="animate-spin text-blue-600" size={20} />
                      <span>Loading sales records...</span>
                    </div>
                  </td>
                </tr>
              ) : sales.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-gray-500">
                    <p className="font-semibold text-base text-gray-700 mb-1">No Sales Invoices Found</p>
                    <p className="text-xs text-gray-400">Try adjusting your search query or create a new sale bill.</p>
                  </td>
                </tr>
              ) : (
                sales.map((sale) => (
                  <tr key={sale.invoiceNumber} className="hover:bg-blue-50/40 transition">
                    <td className="py-3.5 px-4 font-bold text-gray-900">
                      {sale.invoiceNumber}
                    </td>
                    <td className="py-3.5 px-4 font-medium text-gray-800">
                      {sale.customerName}
                    </td>
                    <td className="py-3.5 px-4 text-gray-600 font-mono text-xs">
                      {sale.invoiceDate}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-gray-700 font-semibold">
                      {sale.vehicleNo ? (
                        <span className="px-2 py-0.5 bg-gray-100 text-gray-800 rounded font-mono text-xs uppercase border border-gray-200">
                          {sale.vehicleNo}
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right font-bold font-mono text-gray-900">
                      {formatCurrency(sale.grandTotal)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {/* View & Print Invoice */}
                        <button
                          onClick={() => handleOpenPrintModal(sale)}
                          title="View & Print Invoice"
                          className="p-1.5 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition flex items-center gap-1 text-xs font-semibold px-2 border"
                        >
                          <Printer size={15} /> Print
                        </button>

                        {/* Delete Sale */}
                        {hasWriteAccess && (
                          <button
                            onClick={() => handleDelete(sale)}
                            title={
                              sale.amountPaid && sale.amountPaid > 0
                                ? 'Cannot delete — invoice has recorded payments'
                                : 'Delete Sales Invoice'
                            }
                            disabled={!!(sale.amountPaid && sale.amountPaid > 0)}
                            className={`p-1.5 rounded-lg transition border ${
                              sale.amountPaid && sale.amountPaid > 0
                                ? 'text-gray-300 border-gray-100 cursor-not-allowed'
                                : 'text-gray-600 hover:text-red-600 hover:bg-red-50'
                            }`}
                          >
                            <Trash2 size={15} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Table Footer / Pagination */}
        <div className="py-3 px-4 bg-gray-50 border-t border-gray-200 flex items-center justify-between text-xs text-gray-600">
          <span>Showing {sales.length} entries</span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-1 border border-gray-300 rounded hover:bg-white disabled:opacity-40 transition"
            >
              <ChevronLeft size={16} />
            </button>
            <span className="font-semibold">Page {page + 1} of {totalPages}</span>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-1 border border-gray-300 rounded hover:bg-white disabled:opacity-40 transition"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* Print Invoice Modal */}
      {printModalOpen && printingInvoice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 overflow-y-auto">
          <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full p-6 space-y-4 my-8">
            <div className="flex items-center justify-between border-b pb-3 no-print">
              <h3 className="text-lg font-bold text-gray-900">Sales Invoice Preview</h3>
              <button 
                onClick={() => setPrintModalOpen(false)}
                className="text-gray-500 hover:text-gray-800 p-1.5 rounded-lg border hover:bg-gray-100 transition"
              >
                <X size={20} />
              </button>
            </div>

            <div className="max-h-[75vh] overflow-y-auto border p-2 rounded-lg bg-gray-50">
              <PrintInvoice
                ref={printRef}
                invoice={printingInvoice}
                storageHandle={null}
                companyDetails={companyDetails}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Sales;
