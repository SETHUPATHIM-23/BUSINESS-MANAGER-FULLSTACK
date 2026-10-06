import React, { useState, useEffect, useRef } from 'react';
import { api } from '../utils/api';
import { toastEvents } from '../utils/toast';
import { 
  FileText, Search, RefreshCw, DollarSign, 
  Package, Download, Filter, Printer, Clock, Users, Truck, BookOpen
} from 'lucide-react';
import { ScheduledReportModal } from '../components/reports/ScheduledReportModal';
import { MonthlyBillsDownload } from '../components/reports/MonthlyBillsDownload';
import { getStoredCompanyDetails, fetchCompanySettingsFromBackend } from '../components/billing/CompanySettings';

export const Reports: React.FC = () => {
  const [companyDetails, setCompanyDetails] = useState(() => getStoredCompanyDetails());

  useEffect(() => {
    fetchCompanySettingsFromBackend().then(loaded => setCompanyDetails(loaded));
  }, []);
  const [activeReport, setActiveReport] = useState('sales');
  
  // Shared Filters
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().split('T')[0]);
  
  // Specific entity filters
  const [customerId, setCustomerId] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  
  // Reference data for dropdowns
  const [customers, setCustomers] = useState<any[]>([]);
  const [suppliers, setSuppliers] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [employees, setEmployees] = useState<any[]>([]);

  const [loading, setLoading] = useState(false);
  const [reportData, setReportData] = useState<any>(null);
  
  const [showScheduleModal, setShowScheduleModal] = useState(false);

  useEffect(() => {
    // Load metadata for filters
    const loadMetadata = async () => {
      try {
        const [cRes, sRes, catRes, empRes] = await Promise.all([
          api.get('/api/customers?size=500'),
          api.get('/api/suppliers?size=500'),
          api.get('/api/lookups/categories'),
          api.get('/api/employees?size=500')
        ]);
        setCustomers(cRes.data.content || []);
        setSuppliers(sRes.data.content || []);
        setCategories(catRes.data || []);
        setEmployees(empRes.data.content || []);
      } catch (err) {
        console.error("Failed to load filter metadata");
      }
    };
    loadMetadata();
  }, []);

  interface ReportConfig {
    id: string;
    name: string;
    icon: React.ReactNode;
    group: string;
    singleDate?: boolean;
    noDates?: boolean;
  }

  const reportsList: ReportConfig[] = [
    { id: 'sales', name: 'Sales Report', icon: <DollarSign size={18} />, group: 'Operational' },
    { id: 'purchases', name: 'Purchases Report', icon: <Package size={18} />, group: 'Operational' },
    { id: 'running-sheet', name: 'Employee Running Sheet', icon: <Users size={18} />, group: 'Operational' },
    
    { id: 'customer-statement', name: 'Customer Balance Sheet / Ledger', icon: <FileText size={18} />, group: 'Statements' },
    { id: 'supplier-statement', name: 'Supplier Balance Sheet / Ledger', icon: <FileText size={18} />, group: 'Statements' },

    { id: 'customer-outstanding-summary', name: 'Overall Customer Outstanding', icon: <Users size={18} />, group: 'Outstanding', singleDate: true },
    { id: 'supplier-outstanding-summary', name: 'Overall Supplier Outstanding', icon: <Truck size={18} />, group: 'Outstanding', singleDate: true },

    { id: 'monthly-bills-pdf', name: 'Monthly Bills (Transporter PDF)', icon: <BookOpen size={18} />, group: 'Bulk Download', noDates: true },
  ];

  const currentConfig = reportsList.find(r => r.id === activeReport);

  const fetchReport = async () => {
    if (activeReport === 'customer-statement' && !customerId) {
      toastEvents.error('Please select a Customer for the statement');
      return;
    }
    if (activeReport === 'supplier-statement' && !supplierId) {
      toastEvents.error('Please select a Supplier for the statement');
      return;
    }

    setLoading(true);
    setReportData(null);
    try {
      const params = new URLSearchParams();
      
      if (!currentConfig?.noDates) {
        if (currentConfig?.singleDate) {
          params.append('asOfDate', endDate);
        } else {
          params.append('startDate', startDate);
          params.append('endDate', endDate);
        }
      }
      
      if (customerId) params.append('customerId', customerId);
      if (supplierId) params.append('supplierId', supplierId);
      if (categoryId) params.append('categoryId', categoryId);
      if (employeeId) params.append('employeeId', employeeId);

      const res = await api.get(`/api/reports/${activeReport}?${params.toString()}`);
      setReportData(res.data);
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (format: 'PDF' | 'CSV') => {
    try {
      const params = new URLSearchParams();
      params.append('format', format);
      
      if (!currentConfig?.noDates) {
        if (currentConfig?.singleDate) {
          params.append('asOfDate', endDate);
        } else {
          params.append('startDate', startDate);
          params.append('endDate', endDate);
        }
      }
      if (customerId) params.append('customerId', customerId);
      if (supplierId) params.append('supplierId', supplierId);
      if (categoryId) params.append('categoryId', categoryId);
      if (employeeId) params.append('employeeId', employeeId);

      const endpoint = `/api/reports/${activeReport}/export`;

      toastEvents.info(`Initiating ${format} export...`);
      const response = await api.get(`${endpoint}?${params.toString()}`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${activeReport.replace('/', '_')}_report.${format.toLowerCase()}`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toastEvents.success(`${format} report downloaded successfully!`);
    } catch (err: any) {
      toastEvents.error(`Failed to export ${format}`);
    }
  };

  const reportBodyRef = useRef<HTMLDivElement>(null);

  const handlePrint = async () => {
    // Fetch the exact same PDF as Export PDF, then open it for printing
    try {
      const params = new URLSearchParams();
      params.append('format', 'PDF');

      if (!currentConfig?.noDates) {
        if (currentConfig?.singleDate) {
          params.append('asOfDate', endDate);
        } else {
          params.append('startDate', startDate);
          params.append('endDate', endDate);
        }
      }
      if (customerId) params.append('customerId', customerId);
      if (supplierId) params.append('supplierId', supplierId);
      if (categoryId) params.append('categoryId', categoryId);
      if (employeeId) params.append('employeeId', employeeId);

      toastEvents.info('Preparing PDF for printing...');
      const response = await api.get(`/api/reports/${activeReport}/export?${params.toString()}`, {
        responseType: 'blob'
      });

      const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const printWindow = window.open(blobUrl, '_blank');
      if (printWindow) {
        // Browser PDF viewer will load; print dialog also triggered automatically
        printWindow.onload = () => {
          setTimeout(() => { try { printWindow.print(); } catch {} }, 500);
        };
        toastEvents.success('PDF opened — use the print dialog to print.');
      } else {
        // Popup blocked — trigger download instead
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = `${activeReport}_report.pdf`;
        link.click();
        toastEvents.info('Popup blocked. PDF downloaded instead — open it and print.');
      }
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60000);
    } catch (err: any) {
      toastEvents.error('Failed to prepare PDF for printing.');
    }
  };


  const formatCurrency = (val: number) => 
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val || 0);

  const getReportFinancialSummary = (data: any, reportType: string) => {
    if (!data) return { totalPurchase: 0, totalSale: 0, totalCredit: 0, totalDebit: 0, currentBalance: 0 };

    let totalPurchase = 0;
    let totalSale = 0;
    let totalCredit = 0;
    let totalDebit = 0;
    let currentBalance = 0;

    if (reportType === 'customer-statement') {
      totalSale = Number(data.totalBilledOrPurchased) || 0;
      totalDebit = totalSale;
      totalCredit = Number(data.totalPaidOrSettled) || 0;
      currentBalance = Number(data.closingBalance) || 0;
    } else if (reportType === 'supplier-statement') {
      totalPurchase = Number(data.totalBilledOrPurchased) || 0;
      totalDebit = Number(data.totalPaidOrSettled) || 0;
      totalCredit = 0;
      currentBalance = Number(data.closingBalance) || 0;
    } else if (reportType === 'running-sheet') {
      totalSale = Number(data.totalWorkedAmount) || 0;
      totalDebit = totalSale;
      totalCredit = Number(data.totalSettledAmount) || 0;
      currentBalance = Number(data.closingBalance) || 0;
    } else if (reportType === 'customer-outstanding-summary') {
      totalSale = (data.lines || []).reduce((s: number, l: any) => s + (Number(l.totalBilledOrPurchased) || 0), 0);
      totalDebit = totalSale;
      totalCredit = (data.lines || []).reduce((s: number, l: any) => s + (Number(l.totalPaid) || 0), 0);
      currentBalance = Number(data.totalOutstandingAmount) || 0;
    } else if (reportType === 'supplier-outstanding-summary') {
      totalPurchase = (data.lines || []).reduce((s: number, l: any) => s + (Number(l.totalBilledOrPurchased) || 0), 0);
      totalDebit = (data.lines || []).reduce((s: number, l: any) => s + (Number(l.totalPaid) || 0), 0);
      currentBalance = Number(data.totalOutstandingAmount) || 0;
    } else if (reportType === 'sales') {
      totalSale = Number(data.totalRevenue) || 0;
      totalCredit = totalSale;
      currentBalance = totalSale;
    } else if (reportType === 'purchases') {
      totalPurchase = Number(data.totalPurchases) || 0;
      totalDebit = totalPurchase;
      currentBalance = totalPurchase;
    } else {
      totalPurchase = Number(data.totalPurchases || data.totalPurchased || 0);
      totalSale = Number(data.totalRevenue || data.totalSales || data.totalBilled || 0);
      totalCredit = Number(data.totalCredits || data.totalReceived || data.totalSettled || 0);
      totalDebit = Number(data.totalDebits || data.totalPaid || 0);
      currentBalance = Number(data.closingBalance || data.currentBalance || data.totalOutstandingAmount || 0);
    }

    return { totalPurchase, totalSale, totalCredit, totalDebit, currentBalance };
  };

  return (
    <div className="page-container flex flex-col md:flex-row gap-6 h-full p-6">
      
      {showScheduleModal && <ScheduledReportModal onClose={() => setShowScheduleModal(false)} />}
      
      {/* Sidebar - Report Selection */}
      <div className="w-full md:w-64 flex-shrink-0 flex flex-col gap-2">
        <h2 className="text-xl font-bold mb-1 bg-clip-text text-transparent bg-gradient-to-r from-white to-blue-400">
          Enterprise Reports
        </h2>
        
        <button 
          onClick={() => setShowScheduleModal(true)}
          className="flex items-center justify-center gap-2 mb-3 mt-1 p-2 w-full bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded-lg transition-colors text-sm font-semibold"
        >
          <Clock size={16} /> Manage Schedules
        </button>
        
        {['Operational', 'Statements', 'Outstanding', 'Bulk Download'].map(group => (
          <React.Fragment key={group}>
            <div className="mb-1 text-xs font-bold text-secondary uppercase tracking-wider mt-2">{group}</div>
            {reportsList.filter(r => r.group === group).map(report => (
              <button
                key={report.id}
                onClick={() => { setActiveReport(report.id); setReportData(null); }}
                className={`flex items-center gap-3 p-3 rounded-lg text-left transition-all border ${
                  activeReport === report.id
                    ? 'bg-blue-600/90 text-white border-blue-500 shadow-[0_0_15px_rgba(37,99,235,0.3)]'
                    : 'bg-white/5 border-white/10 hover:bg-white/10'
                }`}
              >
                <span className={activeReport === report.id ? 'text-white' : 'text-blue-400'}>{report.icon}</span>
                <span className="font-medium text-sm">{report.name}</span>
              </button>
            ))}
          </React.Fragment>
        ))}
      </div>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col gap-6">

        {/* ── Monthly Bills Bulk Download (standalone panel, no filter/output) ── */}
        {activeReport === 'monthly-bills-pdf' && (
          <div className="glass-panel p-6 flex-1">
            <MonthlyBillsDownload />
          </div>
        )}

        {/* ── Normal Report Filter + Output (hidden for bulk-download report) ── */}
        {activeReport !== 'monthly-bills-pdf' && (
        <>
        {/* Filter Panel */}
        <div className="glass-panel p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 text-blue-300 pb-2 border-b border-white/10">
            <Filter size={18} />
            <span className="font-semibold uppercase tracking-wider text-sm">Report Filtering & Parameters</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {!currentConfig?.noDates && !currentConfig?.singleDate && (
              <div className="form-group">
                <label className="text-xs text-secondary">Start Date</label>
                <input 
                  type="date" 
                  className="form-input text-sm h-10" 
                  value={startDate} 
                  onChange={e => setStartDate(e.target.value)} 
                />
              </div>
            )}

            {!currentConfig?.noDates && (
              <div className="form-group">
                <label className="text-xs text-secondary">{currentConfig?.singleDate ? 'As Of Date' : 'End Date'}</label>
                <input 
                  type="date" 
                  className="form-input text-sm h-10" 
                  value={endDate} 
                  onChange={e => setEndDate(e.target.value)} 
                />
              </div>
            )}

            {(activeReport === 'sales' || activeReport === 'customer-statement') && (
              <div className="form-group md:col-span-2">
                <label className="text-xs text-secondary">Customer {activeReport === 'customer-statement' && <span className="text-red-400">*</span>}</label>
                <select className="form-input text-sm h-10" value={customerId} onChange={e => setCustomerId(e.target.value)}>
                  <option value="">-- Select Customer --</option>
                  {customers.map(c => <option key={c.id} value={c.id}>{c.customerCode} - {c.name}</option>)}
                </select>
              </div>
            )}

            {(activeReport === 'purchases' || activeReport === 'supplier-statement') && (
              <div className="form-group md:col-span-2">
                <label className="text-xs text-secondary">Supplier {activeReport === 'supplier-statement' && <span className="text-red-400">*</span>}</label>
                <select className="form-input text-sm h-10" value={supplierId} onChange={e => setSupplierId(e.target.value)}>
                  <option value="">-- Select Supplier --</option>
                  {suppliers.map(s => <option key={s.id} value={s.id}>{s.supplierCode} - {s.name}</option>)}
                </select>
              </div>
            )}

            {activeReport === 'running-sheet' && (
              <div className="form-group md:col-span-2">
                <label className="text-xs text-secondary">Employee (Optional - leave blank for All Employees)</label>
                <select className="form-input text-sm h-10" value={employeeId} onChange={e => setEmployeeId(e.target.value)}>
                  <option value="">All Employees</option>
                  {employees.map(emp => <option key={emp.id} value={emp.id}>{emp.employeeCode} - {emp.name}</option>)}
                </select>
              </div>
            )}

            {activeReport === 'inventory-valuation' && (
              <div className="form-group md:col-span-2">
                <label className="text-xs text-secondary">Product Category</label>
                <select className="form-input text-sm h-10" value={categoryId} onChange={e => setCategoryId(e.target.value)}>
                  <option value="">All Categories</option>
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
            )}

            <div className="md:col-span-4 flex justify-end mt-2">
              <button onClick={fetchReport} className="btn-primary flex items-center gap-2 h-10 px-6" disabled={loading}>
                {loading ? <RefreshCw size={16} className="animate-spin" /> : <Search size={16} />}
                {loading ? 'Computing...' : 'Generate Report'}
              </button>
            </div>
          </div>
        </div>

        {/* Report Output Area */}
        <div className="glass-panel flex-1 flex flex-col relative overflow-hidden min-h-[500px]">
          
          {/* Header & Export Actions */}
          <div className="p-4 border-b border-white/10 flex justify-between items-center bg-white/5">
            <h3 className="font-bold text-lg">{currentConfig?.name} Result</h3>
            <div className="flex gap-3">
              <button 
                onClick={() => handleExport('CSV')}
                disabled={!reportData}
                className="btn-secondary text-sm h-9 px-4 flex items-center gap-2"
              >
                <Download size={14} /> Export CSV
              </button>
              <button 
                onClick={() => handleExport('PDF')}
                disabled={!reportData}
                className="btn-secondary text-sm h-9 px-4 flex items-center gap-2"
              >
                <Download size={14} /> Export PDF
              </button>
              <button 
                onClick={handlePrint}
                disabled={!reportData}
                className="btn-primary text-sm h-9 px-4 flex items-center gap-2"
              >
                <Printer size={14} /> Print
              </button>
            </div>
          </div>

          {/* Body */}
          <div ref={reportBodyRef} className="p-6 overflow-auto flex-1">
            {!reportData && !loading && (
              <div className="h-full flex flex-col items-center justify-center text-secondary opacity-50">
                <FileText size={48} className="mb-4 text-white/20" />
                <p>Configure parameters above and click Generate Report.</p>
              </div>
            )}
            
            {loading && (
              <div className="h-full flex items-center justify-center text-secondary gap-3">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-400"></div>
                Computing real-time ledger data...
              </div>
            )}

            {/* Dynamic Rendering - Standardized Executive White A4 Document Sheet Layout */}
            {reportData && (
              <div className="bg-white text-slate-900 border border-gray-300 shadow-2xl rounded-sm p-8 max-w-4xl mx-auto my-4 min-h-[1050px] flex flex-col justify-between font-sans animate-fade-in print:shadow-none print:p-0 print:border-none">
                
                <div className="space-y-6">
                  {/* RUNNING SHEET REPORT VIEW */}
                  {activeReport === 'running-sheet' && (
                    <div className="space-y-6">
                      {/* Header Box / Letterhead */}
                      <div className="flex justify-between items-start pb-4 border-b-2 border-blue-900">
                        <div>
                          <h2 className="text-xl font-extrabold text-blue-950 uppercase tracking-tight">
                            {reportData.ourCompany?.name || companyDetails.name}
                          </h2>
                          <p className="text-xs text-slate-600 mt-1 whitespace-pre-line leading-relaxed">
                            {reportData.ourCompany?.address || companyDetails.address}
                          </p>
                          <p className="text-xs text-slate-600 mt-0.5 font-medium">
                            Phone: <span className="font-bold text-slate-900">{reportData.ourCompany?.phone || companyDetails.mobile}</span> | GSTIN / Tax ID: <span className="font-bold text-slate-900">{reportData.ourCompany?.taxId || companyDetails.gstin}</span>
                          </p>
                        </div>
                        <div className="text-right">
                          <h3 className="text-lg font-black text-blue-900 uppercase tracking-wide">EMPLOYEE RUNNING SHEET</h3>
                          <p className="text-xs font-bold text-slate-700 mt-1">Period: {reportData.startDate} to {reportData.endDate}</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Generated: {new Date(reportData.generatedAt || Date.now()).toLocaleString()}</p>
                        </div>
                      </div>

                      {/* Scope Box */}
                      <div className="p-3 bg-slate-50 border border-slate-300 rounded text-xs">
                        <span className="font-bold uppercase tracking-wider text-slate-700">Scope: </span>
                        {reportData.employee ? (
                          <span className="font-extrabold text-blue-900">{reportData.employee.name} ({reportData.employee.code}) — Dept: {reportData.employee.department || 'General'}</span>
                        ) : (
                          <span className="font-extrabold text-blue-900">All Employees Consolidated Running Sheet</span>
                        )}
                      </div>

                      {/* KPI Metric Summary Grid */}
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        <div className="p-3 bg-slate-50 border border-slate-300 rounded text-center">
                          <p className="text-[10px] text-slate-500 uppercase font-bold">Opening Balance</p>
                          <p className="font-mono text-base font-black text-slate-900 mt-0.5">{formatCurrency(reportData.openingBalance)}</p>
                        </div>
                        <div className="p-3 bg-slate-50 border border-slate-300 rounded text-center">
                          <p className="text-[10px] text-slate-500 uppercase font-bold">Worked Amount</p>
                          <p className="font-mono text-base font-black text-emerald-700 mt-0.5">{formatCurrency(reportData.totalWorkedAmount)}</p>
                        </div>
                        <div className="p-3 bg-slate-50 border border-slate-300 rounded text-center">
                          <p className="text-[10px] text-slate-500 uppercase font-bold">Settled Payouts</p>
                          <p className="font-mono text-base font-black text-blue-700 mt-0.5">{formatCurrency(reportData.totalSettledAmount)}</p>
                        </div>
                        <div className="p-3 bg-blue-50 border border-blue-300 rounded text-center">
                          <p className="text-[10px] text-blue-900 uppercase font-bold">Closing Outstanding</p>
                          <p className="font-mono text-base font-black text-blue-950 mt-0.5">{formatCurrency(reportData.closingBalance)}</p>
                        </div>
                      </div>

                      {/* Detailed Grid Table */}
                      <div className="overflow-x-auto border border-slate-400 rounded-sm">
                        <table className="w-full border-collapse text-xs">
                          <thead>
                            <tr className="bg-slate-900 text-white font-bold uppercase text-[11px]">
                              <th className="p-2.5 text-left border-r border-slate-700">Date</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Employee</th>
                              <th className="p-2.5 text-center border-r border-slate-700">Type</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Description / Ref</th>
                              <th className="p-2.5 text-right border-r border-slate-700">Worked</th>
                              <th className="p-2.5 text-right border-r border-slate-700">Settled</th>
                              <th className="p-2.5 text-right">Net Balance</th>
                            </tr>
                          </thead>
                          <tbody>
                            {reportData.lines && reportData.lines.length > 0 ? (
                              reportData.lines.map((line: any, i: number) => (
                                <tr key={i} className={`border-b border-slate-300 ${i % 2 === 1 ? 'bg-slate-50' : 'bg-white'}`}>
                                  <td className="p-2.5 border-r border-slate-300 text-slate-800 font-semibold">{line.date}</td>
                                  <td className="p-2.5 border-r border-slate-300 font-bold text-slate-900">{line.employeeCode} - {line.employeeName}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-center">
                                    <span className={`px-1.5 py-0.5 rounded text-[10px] font-black uppercase ${line.entryType === 'WORK' ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-blue-100 text-blue-900 border border-blue-300'}`}>
                                      {line.entryType}
                                    </span>
                                  </td>
                                  <td className="p-2.5 border-r border-slate-300 text-slate-700">
                                    {line.description}
                                    {line.referenceNo ? ` (Ref: ${line.referenceNo})` : ''}
                                    {line.paymentMode && line.entryType === 'SETTLEMENT' ? ` [${line.paymentMode}]` : ''}
                                  </td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-emerald-800">
                                    {line.workedAmount > 0 ? formatCurrency(line.workedAmount) : '-'}
                                  </td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-blue-800">
                                    {line.settledAmount > 0 ? formatCurrency(line.settledAmount) : '-'}
                                  </td>
                                  <td className="p-2.5 text-right font-mono font-black text-slate-950">
                                    {formatCurrency(line.runningBalance)}
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan={7} className="p-6 text-center text-slate-500 font-medium">
                                  No transaction lines found for the selected period.
                                </td>
                              </tr>
                            )}
                          </tbody>
                          <tfoot className="bg-slate-900 text-white font-bold text-xs uppercase border-t-2 border-slate-700">
                            <tr>
                              <td colSpan={4} className="p-2.5 text-right border-r border-slate-700 font-extrabold">TOTALS:</td>
                              <td className="p-2.5 text-right font-mono text-emerald-400 border-r border-slate-700 font-black">{formatCurrency(reportData.totalWorkedAmount || 0)}</td>
                              <td className="p-2.5 text-right font-mono text-blue-400 border-r border-slate-700 font-black">{formatCurrency(reportData.totalSettledAmount || 0)}</td>
                              <td className="p-2.5 text-right font-mono text-yellow-300 font-black">{formatCurrency(reportData.closingBalance || 0)}</td>
                            </tr>
                          </tfoot>
                        </table>
                      </div>
                    </div>
                  )}

                  {/* 1. CUSTOMER / SUPPLIER RUNNING BALANCE STATEMENT VIEW */}
                  {(activeReport === 'customer-statement' || activeReport === 'supplier-statement') && (
                    <div className="space-y-6">
                      {/* Header Box / Letterhead */}
                      <div className="flex justify-between items-start pb-4 border-b-2 border-blue-900">
                        <div>
                          <h2 className="text-xl font-extrabold text-blue-950 uppercase tracking-tight">
                            {reportData.ourCompany?.name || companyDetails.name}
                          </h2>
                          <p className="text-xs text-slate-600 mt-1 whitespace-pre-line leading-relaxed">
                            {reportData.ourCompany?.address || companyDetails.address}
                          </p>
                          <p className="text-xs text-slate-600 mt-0.5 font-medium">
                            Phone: <span className="font-bold text-slate-900">{reportData.ourCompany?.phone || companyDetails.mobile}</span> | GSTIN / Tax ID: <span className="font-bold text-slate-900">{reportData.ourCompany?.taxId || companyDetails.gstin}</span>
                          </p>
                        </div>
                        <div className="text-right">
                          <h3 className="text-lg font-black text-blue-900 uppercase tracking-wide">
                            {activeReport === 'customer-statement' ? 'CUSTOMER STATEMENT' : 'SUPPLIER STATEMENT'}
                          </h3>
                          <p className="text-xs font-bold text-slate-700 mt-1">Period: {startDate} to {endDate}</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Generated: {new Date(reportData.generatedAt || Date.now()).toLocaleString()}</p>
                        </div>
                      </div>

                      {/* Partner Details Card */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-3 bg-slate-50 border border-slate-300 rounded text-xs">
                        <div>
                          <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">{activeReport === 'customer-statement' ? 'Customer Details' : 'Supplier Details'}</p>
                          <p className="font-black text-blue-950 text-sm">{reportData.partner?.name} ({reportData.partner?.code})</p>
                          <p className="text-slate-600 mt-0.5">{reportData.partner?.address || 'No address on file'}</p>
                          <p className="text-slate-600">Phone: {reportData.partner?.phone || '-'} | Email: {reportData.partner?.email || '-'}</p>
                        </div>
                        <div className="md:text-right flex flex-col justify-between">
                          <div>
                            <p className="text-[10px] uppercase font-bold text-slate-500 mb-1">Statement Summary</p>
                            <p className="text-slate-700">Opening Balance: <span className="font-mono font-bold text-slate-900">{formatCurrency(reportData.openingBalance)}</span></p>
                            <p className="text-slate-700">{activeReport === 'customer-statement' ? 'Total Billed:' : 'Total Purchased:'} <span className="font-mono font-bold text-blue-900">{formatCurrency(reportData.totalBilledOrPurchased)}</span></p>
                            <p className="text-slate-700">{activeReport === 'customer-statement' ? 'Total Received:' : 'Total Paid:'} <span className="font-mono font-bold text-emerald-800">{formatCurrency(reportData.totalPaidOrSettled)}</span></p>
                          </div>
                          <p className="text-sm font-black text-slate-950 mt-1 pt-1 border-t border-slate-300">
                            Net Balance: <span className="font-mono text-red-700">{formatCurrency(reportData.closingBalance)}</span>
                          </p>
                        </div>
                      </div>

                      {/* Detailed Grid Table */}
                      <div className="overflow-x-auto border border-slate-400 rounded-sm">
                        <table className="w-full border-collapse text-xs">
                          <thead>
                            <tr className="bg-slate-900 text-white font-bold uppercase text-[11px]">
                              <th className="p-2.5 text-left border-r border-slate-700">Date</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Doc / Ref No</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Description</th>
                              <th className="p-2.5 text-right border-r border-slate-700">{activeReport === 'customer-statement' ? 'Billed (DR)' : 'Purchased (DR)'}</th>
                              <th className="p-2.5 text-right border-r border-slate-700">{activeReport === 'customer-statement' ? 'Received (CR)' : 'Paid (CR)'}</th>
                              <th className="p-2.5 text-right">Running Balance</th>
                            </tr>
                          </thead>
                          <tbody>
                            {reportData.lines && reportData.lines.length > 0 ? (
                              reportData.lines.map((line: any, i: number) => (
                                <tr key={i} className={`border-b border-slate-300 ${i % 2 === 1 ? 'bg-slate-50' : 'bg-white'}`}>
                                  <td className="p-2.5 border-r border-slate-300 text-slate-800 font-semibold">{line.date}</td>
                                  <td className="p-2.5 border-r border-slate-300 font-mono font-bold text-blue-900">{line.documentCode}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-slate-700">{line.description}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-blue-800">
                                    {line.billedOrPurchasedAmount > 0 ? formatCurrency(line.billedOrPurchasedAmount) : '-'}
                                  </td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-emerald-800">
                                    {line.paidAmount > 0 ? formatCurrency(line.paidAmount) : '-'}
                                  </td>
                                  <td className="p-2.5 text-right font-mono font-black text-slate-950">
                                    {formatCurrency(line.runningBalance)}
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan={6} className="p-6 text-center text-slate-500 font-medium">
                                  No transaction lines found for the selected period.
                                </td>
                              </tr>
                            )}
                          </tbody>
                          <tfoot className="bg-slate-900 text-white font-bold text-xs uppercase border-t-2 border-slate-700">
                            <tr>
                              <td colSpan={3} className="p-2.5 text-right border-r border-slate-700 font-extrabold">TOTALS:</td>
                              <td className="p-2.5 text-right font-mono text-blue-300 border-r border-slate-700 font-black">
                                {formatCurrency(reportData.totalBilledOrPurchased || 0)}
                              </td>
                              <td className="p-2.5 text-right font-mono text-emerald-300 border-r border-slate-700 font-black">
                                {formatCurrency(reportData.totalPaidOrSettled || 0)}
                              </td>
                              <td className="p-2.5 text-right font-mono text-yellow-300 font-black">
                                {formatCurrency(reportData.closingBalance || 0)}
                              </td>
                            </tr>
                          </tfoot>
                        </table>
                      </div>
                    </div>
                  )}

                  {/* 2. OVERALL CUSTOMER / SUPPLIER OUTSTANDING SUMMARY VIEW */}
                  {(activeReport === 'customer-outstanding-summary' || activeReport === 'supplier-outstanding-summary') && (
                    <div className="space-y-6">
                      {/* Header Box / Letterhead */}
                      <div className="flex justify-between items-start pb-4 border-b-2 border-blue-900">
                        <div>
                          <h2 className="text-xl font-extrabold text-blue-950 uppercase tracking-tight">
                            {reportData.ourCompany?.name || companyDetails.name}
                          </h2>
                          <p className="text-xs text-slate-600 mt-1 whitespace-pre-line leading-relaxed">
                            {reportData.ourCompany?.address || companyDetails.address}
                          </p>
                          <p className="text-xs text-slate-600 mt-0.5 font-medium">
                            Phone: <span className="font-bold text-slate-900">{reportData.ourCompany?.phone || companyDetails.mobile}</span> | GSTIN / Tax ID: <span className="font-bold text-slate-900">{reportData.ourCompany?.taxId || companyDetails.gstin}</span>
                          </p>
                        </div>
                        <div className="text-right">
                          <h3 className="text-lg font-black text-blue-900 uppercase tracking-wide">
                            {activeReport === 'customer-outstanding-summary' ? 'OVERALL CUSTOMER OUTSTANDING' : 'OVERALL SUPPLIER OUTSTANDING'}
                          </h3>
                          <p className="text-xs font-bold text-slate-700 mt-1">As of: {endDate}</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Generated: {new Date(reportData.generatedAt || Date.now()).toLocaleString()}</p>
                        </div>
                      </div>

                      {/* KPI Grid */}
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                        <div className="p-3 bg-red-50 border border-red-300 rounded text-center">
                          <p className="text-[10px] text-red-900 uppercase font-bold">Total Outstanding {activeReport === 'customer-outstanding-summary' ? 'Receivables' : 'Payables'}</p>
                          <p className="font-mono text-lg font-black text-red-700 mt-0.5">{formatCurrency(reportData.totalOutstandingAmount)}</p>
                        </div>
                        <div className="p-3 bg-slate-50 border border-slate-300 rounded text-center">
                          <p className="text-[10px] text-slate-500 uppercase font-bold">{activeReport === 'customer-outstanding-summary' ? 'Customers With Dues' : 'Suppliers Pending'}</p>
                          <p className="font-mono text-lg font-black text-slate-900 mt-0.5">{reportData.partnersWithDueCount} Records</p>
                        </div>
                        <div className="p-3 bg-slate-50 border border-slate-300 rounded text-center">
                          <p className="text-[10px] text-slate-500 uppercase font-bold">Total Registered Accounts</p>
                          <p className="font-mono text-lg font-black text-slate-900 mt-0.5">{reportData.totalPartnersCount} Records</p>
                        </div>
                      </div>

                      {/* Detailed Grid Table */}
                      <div className="overflow-x-auto border border-slate-400 rounded-sm">
                        <table className="w-full border-collapse text-xs">
                          <thead>
                            <tr className="bg-slate-900 text-white font-bold uppercase text-[11px]">
                              <th className="p-2.5 text-left border-r border-slate-700">Code</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Partner Name</th>
                              <th className="p-2.5 text-left border-r border-slate-700">Contact</th>
                              <th className="p-2.5 text-right border-r border-slate-700">{activeReport === 'customer-outstanding-summary' ? 'Total Billed' : 'Total Purchased'}</th>
                              <th className="p-2.5 text-right border-r border-slate-700">{activeReport === 'customer-outstanding-summary' ? 'Total Received' : 'Total Paid'}</th>
                              <th className="p-2.5 text-right">Outstanding Balance</th>
                            </tr>
                          </thead>
                          <tbody>
                            {reportData.lines && reportData.lines.length > 0 ? (
                              reportData.lines.map((line: any, i: number) => (
                                <tr key={i} className={`border-b border-slate-300 ${i % 2 === 1 ? 'bg-slate-50' : 'bg-white'}`}>
                                  <td className="p-2.5 border-r border-slate-300 font-mono font-bold text-blue-900">{line.partnerCode}</td>
                                  <td className="p-2.5 border-r border-slate-300 font-bold text-slate-900">{line.partnerName}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-slate-700">{line.phone || line.email || '-'}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-blue-800">{formatCurrency(line.totalBilledOrPurchased)}</td>
                                  <td className="p-2.5 border-r border-slate-300 text-right font-mono font-bold text-emerald-800">{formatCurrency(line.totalPaid)}</td>
                                  <td className={`p-2.5 text-right font-mono font-black ${line.outstandingBalance > 0 ? 'text-red-700' : 'text-emerald-800'}`}>
                                    {formatCurrency(line.outstandingBalance)}
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan={6} className="p-6 text-center text-slate-500 font-medium">No records found.</td>
                              </tr>
                            )}
                          </tbody>
                          <tfoot className="bg-slate-900 text-white font-bold text-xs uppercase border-t-2 border-slate-700">
                            <tr>
                              <td colSpan={3} className="p-2.5 text-right border-r border-slate-700 font-extrabold">GRAND TOTALS:</td>
                              <td className="p-2.5 text-right font-mono text-blue-300 border-r border-slate-700 font-black">
                                {formatCurrency((reportData.lines || []).reduce((s: number, l: any) => s + (Number(l.totalBilledOrPurchased) || 0), 0))}
                              </td>
                              <td className="p-2.5 text-right font-mono text-emerald-300 border-r border-slate-700 font-black">
                                {formatCurrency((reportData.lines || []).reduce((s: number, l: any) => s + (Number(l.totalPaid) || 0), 0))}
                              </td>
                              <td className="p-2.5 text-right font-mono text-red-400 font-black">
                                {formatCurrency(reportData.totalOutstandingAmount || 0)}
                              </td>
                            </tr>
                          </tfoot>
                        </table>
                      </div>
                    </div>
                  )}

                  {/* 3. GENERIC / OPERATIONAL REPORTS VIEW */}
                  {activeReport !== 'customer-statement' && activeReport !== 'supplier-statement' && 
                   activeReport !== 'customer-outstanding-summary' && activeReport !== 'supplier-outstanding-summary' && 
                   activeReport !== 'running-sheet' && (
                    <div className="space-y-6">
                      {/* Header Box / Letterhead */}
                      <div className="flex justify-between items-start pb-4 border-b-2 border-blue-900">
                        <div>
                          <h2 className="text-xl font-extrabold text-blue-950 uppercase tracking-tight">
                            {reportData.ourCompany?.name || companyDetails.name}
                          </h2>
                          <p className="text-xs text-slate-600 mt-1 whitespace-pre-line leading-relaxed">
                            {reportData.ourCompany?.address || companyDetails.address}
                          </p>
                          <p className="text-xs text-slate-600 mt-0.5 font-medium">
                            Phone: <span className="font-bold text-slate-900">{reportData.ourCompany?.phone || companyDetails.mobile}</span> | GSTIN / Tax ID: <span className="font-bold text-slate-900">{reportData.ourCompany?.taxId || companyDetails.gstin}</span>
                          </p>
                        </div>
                        <div className="text-right">
                          <h3 className="text-lg font-black text-blue-900 uppercase tracking-wide">{currentConfig?.name || activeReport.toUpperCase()}</h3>
                          <p className="text-xs font-bold text-slate-700 mt-1">Period: {startDate} to {endDate}</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Generated: {new Date().toLocaleString()}</p>
                        </div>
                      </div>

                      {/* KPI Summary Cards */}
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-3 bg-slate-50 border border-slate-300 rounded">
                        {Object.entries(reportData).map(([key, value]) => {
                          if (typeof value === 'number') {
                            return (
                              <div key={key} className="text-center p-2 bg-white border border-slate-200 rounded">
                                <p className="text-[10px] text-slate-500 uppercase font-bold">{key.replace(/([A-Z])/g, ' $1').trim()}</p>
                                <p className={`font-mono text-base font-black mt-0.5 ${value < 0 ? 'text-red-700' : 'text-slate-950'}`}>
                                  {formatCurrency(value)}
                                </p>
                              </div>
                            );
                          }
                          return null;
                        })}
                      </div>

                      {/* Detailed Grid Table */}
                      {reportData.lines && reportData.lines.length > 0 && (
                        <div className="overflow-x-auto border border-slate-400 rounded-sm">
                          <table className="w-full border-collapse text-xs">
                            <thead>
                              <tr className="bg-slate-900 text-white font-bold uppercase text-[11px]">
                                {Object.keys(reportData.lines[0]).map(k => (
                                  <th key={k} className="p-2.5 text-left border-r border-slate-700">
                                    {k.replace(/([A-Z])/g, ' $1').trim()}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody>
                              {reportData.lines.map((line: any, i: number) => (
                                <tr key={i} className={`border-b border-slate-300 ${i % 2 === 1 ? 'bg-slate-50' : 'bg-white'}`}>
                                  {Object.values(line).map((v: any, j: number) => (
                                    <td key={j} className={`p-2.5 border-r border-slate-300 ${typeof v === 'number' ? 'font-mono text-right font-bold text-slate-950' : 'text-slate-800'}`}>
                                      {typeof v === 'number' ? formatCurrency(v) : (v || '-')}
                                    </td>
                                  ))}
                                </tr>
                              ))}
                            </tbody>
                            <tfoot className="bg-slate-900 text-white font-bold text-xs uppercase border-t-2 border-slate-700">
                              <tr>
                                {Object.keys(reportData.lines[0]).map((k, idx) => {
                                  const isNumeric = typeof reportData.lines[0][k] === 'number';
                                  const colTotal = isNumeric ? reportData.lines.reduce((s: number, l: any) => s + (Number(l[k]) || 0), 0) : null;
                                  return (
                                    <td key={idx} className={`p-2.5 border-r border-slate-700 ${isNumeric ? 'text-right font-mono text-yellow-300 font-black' : 'font-extrabold text-slate-300'}`}>
                                      {idx === 0 ? 'TOTALS:' : (isNumeric ? formatCurrency(colTotal!) : '')}
                                    </td>
                                  );
                                })}
                              </tr>
                            </tfoot>
                          </table>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* CONSOLIDATED FINANCIAL SUMMARY FOOTER BAR */}
                {(() => {
                  const summary = getReportFinancialSummary(reportData, activeReport);
                  return (
                    <div className="mt-6 p-4 bg-slate-900 text-white rounded-sm border border-slate-800 shadow-md page-break-inside-avoid">
                      <div className="flex justify-between items-center pb-2 mb-3 border-b border-slate-800">
                        <h4 className="text-[11px] font-black uppercase tracking-wider text-blue-400">
                          REPORT FINANCIAL SUMMARY FOOTER
                        </h4>
                        <span className="text-[10px] text-slate-400 font-semibold uppercase">
                          {currentConfig?.name || activeReport}
                        </span>
                      </div>
                      
                      <div className="grid grid-cols-2 md:grid-cols-5 gap-2 text-center">
                        <div className="p-2.5 bg-slate-800/90 border border-slate-700 rounded">
                          <p className="text-[9px] uppercase font-extrabold text-slate-400">Total Purchase</p>
                          <p className="font-mono text-sm font-black text-blue-300 mt-1">
                            {formatCurrency(summary.totalPurchase)}
                          </p>
                        </div>
                        
                        <div className="p-2.5 bg-slate-800/90 border border-slate-700 rounded">
                          <p className="text-[9px] uppercase font-extrabold text-slate-400">Total Sale</p>
                          <p className="font-mono text-sm font-black text-amber-300 mt-1">
                            {formatCurrency(summary.totalSale)}
                          </p>
                        </div>

                        <div className="p-2.5 bg-slate-800/90 border border-slate-700 rounded">
                          <p className="text-[9px] uppercase font-extrabold text-slate-400">Total Credit (CR)</p>
                          <p className="font-mono text-sm font-black text-emerald-300 mt-1">
                            {formatCurrency(summary.totalCredit)}
                          </p>
                        </div>

                        <div className="p-2.5 bg-slate-800/90 border border-slate-700 rounded">
                          <p className="text-[9px] uppercase font-extrabold text-slate-400">Total Debit (DR)</p>
                          <p className="font-mono text-sm font-black text-rose-300 mt-1">
                            {formatCurrency(summary.totalDebit)}
                          </p>
                        </div>

                        <div className="p-2.5 bg-blue-950 border border-blue-600 rounded">
                          <p className="text-[9px] uppercase font-extrabold text-blue-300">Current Balance</p>
                          <p className="font-mono text-sm font-black text-white mt-1">
                            {formatCurrency(summary.currentBalance)}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })()}

                {/* Standardized Signatures Block & Footnote */}
                <div className="pt-8 border-t border-slate-300 mt-8 space-y-6">
                  <div className="flex justify-between items-end px-6 text-xs text-slate-800">
                    <div className="text-center">
                      <div className="w-40 border-b border-dashed border-slate-400 mb-1"></div>
                      <p className="font-extrabold uppercase text-[10px] text-slate-700 tracking-wider">Receiver Signatory</p>
                      <p className="text-[9px] text-slate-400">Signature &amp; Stamp</p>
                    </div>
                    <div className="text-center">
                      <div className="w-40 border-b border-dashed border-slate-400 mb-1"></div>
                      <p className="font-extrabold uppercase text-[10px] text-slate-700 tracking-wider">Authorized Signatory</p>
                      <p className="text-[9px] text-slate-500 font-bold">{companyDetails.name}</p>
                    </div>
                  </div>

                  <div className="text-center text-[10px] text-slate-400 pt-2 border-t border-slate-200 uppercase tracking-wider font-semibold">
                    Computer Generated Enterprise Report | Printed: {new Date().toLocaleString()} | Confidential
                  </div>
                </div>

              </div>
            )}
          </div>
        </div>
        </>
        )}{/* end activeReport !== 'monthly-bills-pdf' */}
      </div>
    </div>
  );
};
