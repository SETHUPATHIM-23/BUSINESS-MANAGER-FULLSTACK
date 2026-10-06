import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { 
  ArrowLeft, Calendar, FileText, CheckCircle, AlertTriangle, 
  Printer, Download, RefreshCw, FileSearch, Lock, Unlock, Building2
} from 'lucide-react';

interface SupplierOption {
  id: number;
  supplierCode: string;
  name: string;
}

interface StatementEntry {
  date: string;
  documentCode: string;
  description: string;
  type: 'PURCHASE_INVOICE' | 'PURCHASE_ORDER' | 'PAYMENT' | 'SETTLEMENT' | 'DEBIT_NOTE';
  amount: number;
}

interface StatementResponse {
  supplierCode: string;
  supplierName: string;
  bankAccountDetails?: string;
  openingBalance: number;
  closingBalance: number;
  startDate: string;
  endDate: string;
  entries: StatementEntry[];
  totalUnpaidInvoices: number;
  totalUnappliedDebits: number;
  reconciledBalance: number;
}

export const SupplierStatement: React.FC = () => {
  const { supplierId } = useParams<{ supplierId?: string }>();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();

  // Role Gate: Bank account details are restricted to Accountants/Administrators (SUPP-040)
  const showBankDetails = hasPermission('ACCOUNTING_READ') || hasPermission('SYSTEM_READ') || hasPermission('SYSTEM_WRITE');

  // Search & Filter state
  const [suppliers, setSuppliers] = useState<SupplierOption[]>([]);
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>(supplierId || '');
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().split('T')[0]);

  // Report state
  const [statement, setStatement] = useState<StatementResponse | null>(null);
  const [loading, setLoading] = useState(false);

  // Fetch suppliers for selector
  useEffect(() => {
    const fetchSuppliers = async () => {
      try {
        const response = await api.get('/api/suppliers', { params: { size: 100 } });
        setSuppliers(response.data.content || []);
      } catch (err) {
        console.error('Failed to load suppliers for selector', err);
      }
    };
    fetchSuppliers();
  }, []);

  useEffect(() => {
    if (supplierId) {
      setSelectedSupplierId(supplierId);
    }
  }, [supplierId]);

  const handleGenerate = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!selectedSupplierId) {
      toastEvents.error('Please select a supplier');
      return;
    }

    setLoading(true);
    setStatement(null);
    try {
      const response = await api.get<StatementResponse>(`/api/suppliers/${selectedSupplierId}/statement`, {
        params: { startDate, endDate }
      });
      setStatement(response.data);
    } catch (err) {
      console.error('Failed to generate statement', err);
      toastEvents.error('Failed to retrieve supplier statement records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (supplierId && suppliers.length > 0) {
      handleGenerate();
    }
  }, [supplierId, suppliers]);

  // Format description dynamically without # symbols
  const formatNarration = (rawDesc: string, type: string, docCode: string) => {
    let clean = (rawDesc || '').replace(/#/g, '').trim();
    if (type === 'PURCHASE_ORDER' || type === 'PURCHASE_INVOICE' || type === 'PURCHASE') {
      if (clean.startsWith('Purchase Order') || clean.startsWith('PO')) {
        const poNo = clean.replace(/^(Purchase Order|PO)\s*/i, '').trim();
        return `Invoiced - PO No: ${poNo || docCode}`;
      }
      if (!clean.toLowerCase().includes('invoiced') && !clean.toLowerCase().includes('purchased')) {
        return `Invoiced - PO No: ${docCode || clean}`;
      }
      return clean;
    }
    if (type === 'PAYMENT' || type === 'SETTLEMENT') {
      if (!clean.toLowerCase().includes('payment paid') && !clean.toLowerCase().includes('settled')) {
        return `Payment Paid ${docCode ? `- Against PO No: ${docCode.replace('SETTLE-', '').replace('PAY-', '')}` : ''}`;
      }
      return clean;
    }
    return clean || 'Supplier Transaction';
  };

  // Compute chronological running balance starting from opening balance
  const computeRunningBalances = (opening: number, entries: StatementEntry[]) => {
    let current = opening;
    return entries.map((entry) => {
      current = current + entry.amount;
      return {
        ...entry,
        formattedDescription: formatNarration(entry.description, entry.type, entry.documentCode),
        runningBalance: current
      };
    });
  };

  const calculatedEntries = statement 
    ? computeRunningBalances(statement.openingBalance, statement.entries) 
    : [];

  const isReconciled = statement
    ? statement.reconciledBalance === statement.closingBalance
    : false;

  return (
    <div style={{ width: '100%', padding: '1.5rem', maxWidth: '1280px', margin: '0 auto' }}>
      {/* Back button */}
      <button 
        onClick={() => navigate('/suppliers')}
        style={{
          background: 'none',
          border: 'none',
          color: 'var(--text-muted)',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          fontSize: '0.88rem',
          marginBottom: '1.5rem',
          transition: 'var(--transition-smooth)'
        }}
        onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
      >
        <ArrowLeft size={16} />
        <span>Back to Supplier Directory</span>
      </button>

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
            Supplier Balance Sheet & Accounts Payable Ledger
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            Chronological purchase orders, supplier bank payments, debit adjustments, and audit ledger reconciliation
          </p>
        </div>
      </div>

      {/* Selector Box */}
      <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
        <form onSubmit={handleGenerate} style={{ display: 'flex', gap: '1.25rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          {/* Supplier */}
          <div style={{ flex: 1, minWidth: '240px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Select Supplier Vendor
            </label>
            <select
              value={selectedSupplierId}
              onChange={(e) => setSelectedSupplierId(e.target.value)}
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: '8px',
                border: '1px solid var(--border-color)',
                backgroundColor: '#111827',
                color: '#fff',
                fontSize: '0.9rem',
                outline: 'none',
                cursor: 'pointer'
              }}
            >
              <option value="">-- Select Supplier Vendor --</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.supplierCode} - {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Start Date */}
          <div style={{ width: '190px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              From Date
            </label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <Calendar size={16} style={{ position: 'absolute', left: '12px', color: 'var(--text-muted)' }} />
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 36px',
                  borderRadius: '8px',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'rgba(255,255,255,0.02)',
                  color: '#fff',
                  fontSize: '0.88rem',
                  outline: 'none'
                }}
              />
            </div>
          </div>

          {/* End Date */}
          <div style={{ width: '190px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              To Date
            </label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <Calendar size={16} style={{ position: 'absolute', left: '12px', color: 'var(--text-muted)' }} />
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 36px',
                  borderRadius: '8px',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'rgba(255,255,255,0.02)',
                  color: '#fff',
                  fontSize: '0.88rem',
                  outline: 'none'
                }}
              />
            </div>
          </div>

          {/* Button */}
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading}
            style={{ height: '42px', display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: '170px', justifyContent: 'center', fontWeight: 700 }}
          >
            {loading ? <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <FileSearch size={16} />}
            <span>Compile Statement</span>
          </button>
        </form>
      </div>

      {/* Statement Output */}
      {loading && !statement ? (
        <div className="glass-panel" style={{ padding: '6rem', textAlign: 'center' }}>
          <RefreshCw size={36} style={{ animation: 'spin 1s linear infinite', color: 'var(--accent-primary)', marginBottom: '1.5rem' }} />
          <h3 style={{ fontSize: '1.15rem', color: '#fff', fontWeight: 600 }}>Compiling Accounts Payable Ledger</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', marginTop: '0.5rem' }}>Processing purchase orders, bank settlements, and running AP balance totals...</p>
        </div>
      ) : statement ? (
        <div className="glass-panel statement-print-area" style={{ padding: '2.5rem', backgroundColor: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)' }}>
          
          {/* Action Tools */}
          <div className="no-print" style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <button className="btn btn-secondary" style={{ padding: '8px 16px', display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 600 }} onClick={() => window.print()}>
              <Printer size={15} />
              <span>Print Statement</span>
            </button>
            <button className="btn btn-secondary" style={{ padding: '8px 16px', display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 600 }} onClick={() => window.print()}>
              <Download size={15} />
              <span>Save PDF</span>
            </button>
          </div>

          {/* Corporate Header Banner */}
          <div style={{ borderBottom: '2px solid rgba(255,255,255,0.1)', paddingBottom: '1.75rem', marginBottom: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#38bdf8', fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '0.35rem' }}>
                  <Building2 size={16} />
                  <span>SENTHUR CHEMICAL — SUPPLIER AP LEDGER STATEMENT</span>
                </div>
                <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.02em' }}>{statement.supplierName}</h2>
                <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.5rem', fontSize: '0.85rem', color: '#94a3b8' }}>
                  <span>Supplier Code: <strong style={{ color: '#f8fafc' }}>{statement.supplierCode}</strong></span>
                  <span>Currency: <strong style={{ color: '#f8fafc' }}>INR (₹)</strong></span>
                </div>
              </div>

              <div style={{ textAlign: 'right', backgroundColor: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px', padding: '0.85rem 1.25rem' }}>
                <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'block' }}>Statement Period</span>
                <span style={{ fontSize: '1rem', color: '#38bdf8', fontWeight: 700, marginTop: '2px', display: 'block' }}>
                  {statement.startDate} to {statement.endDate}
                </span>
                <span style={{ fontSize: '0.72rem', color: '#64748b', marginTop: '4px', display: 'block' }}>
                  Generated on {new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                </span>
              </div>
            </div>

            {/* Bank details panel - Role restricted display */}
            <div style={{ 
              marginTop: '1.25rem', 
              padding: '10px 14px', 
              borderRadius: '8px', 
              backgroundColor: 'rgba(255,255,255,0.02)', 
              border: '1px solid rgba(255,255,255,0.08)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'space-between',
              maxWidth: '540px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                {showBankDetails ? (
                  <Unlock size={16} style={{ color: '#34d399' }} />
                ) : (
                  <Lock size={16} style={{ color: '#94a3b8' }} />
                )}
                <div>
                  <span style={{ display: 'block', fontSize: '0.7rem', color: '#94a3b8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Remittance Bank Account (Encrypted at Rest)
                  </span>
                  <span style={{ display: 'block', fontSize: '0.85rem', color: showBankDetails ? '#f8fafc' : '#94a3b8', fontFamily: showBankDetails ? 'monospace' : 'inherit', marginTop: '2px' }}>
                    {showBankDetails 
                      ? (statement.bankAccountDetails || 'No bank account details provided.')
                      : '•••••••••••••••••••••••• (Restricted to Finance Roles)'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Balances Overview Blocks */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: '1.25rem', marginBottom: '2.25rem' }}>
            <div style={{ backgroundColor: 'rgba(15, 23, 42, 0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '10px', padding: '1.25rem' }}>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Opening AP Balance</span>
              <h3 style={{ fontSize: '1.6rem', color: '#f8fafc', marginTop: '0.5rem', fontWeight: 700 }}>
                ₹ {statement.openingBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </h3>
              <span style={{ fontSize: '0.72rem', color: '#64748b', marginTop: '4px', display: 'block' }}>Opening payable balance</span>
            </div>

            <div style={{ backgroundColor: 'rgba(15, 23, 42, 0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '10px', padding: '1.25rem' }}>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total Invoiced Purchases</span>
              <h3 style={{ fontSize: '1.6rem', color: '#fbbf24', marginTop: '0.5rem', fontWeight: 700 }}>
                ₹ {calculatedEntries.filter(e => e.amount > 0).reduce((sum, e) => sum + e.amount, 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </h3>
              <span style={{ fontSize: '0.72rem', color: '#64748b', marginTop: '4px', display: 'block' }}>Total PO billing additions</span>
            </div>

            <div style={{ backgroundColor: 'rgba(15, 23, 42, 0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '10px', padding: '1.25rem' }}>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total Payments Paid</span>
              <h3 style={{ fontSize: '1.6rem', color: '#34d399', marginTop: '0.5rem', fontWeight: 700 }}>
                ₹ {Math.abs(calculatedEntries.filter(e => e.amount < 0).reduce((sum, e) => sum + e.amount, 0)).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </h3>
              <span style={{ fontSize: '0.72rem', color: '#64748b', marginTop: '4px', display: 'block' }}>Bank/Cash settlements debited</span>
            </div>

            <div style={{ backgroundColor: 'rgba(15, 23, 42, 0.8)', border: '1px solid rgba(56, 189, 248, 0.3)', borderRadius: '10px', padding: '1.25rem', background: 'linear-gradient(135deg, rgba(15,23,42,0.9) 0%, rgba(30,58,138,0.2) 100%)' }}>
              <span style={{ fontSize: '0.75rem', color: '#38bdf8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Closing AP Balance Due</span>
              <h3 style={{ fontSize: '1.65rem', color: '#ffffff', marginTop: '0.5rem', fontWeight: 800 }}>
                ₹ {statement.closingBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </h3>
              <span style={{ fontSize: '0.72rem', color: '#94a3b8', marginTop: '4px', display: 'block' }}>Net Accounts Payable balance</span>
            </div>
          </div>

          {/* Ledger Table */}
          <div style={{ border: '1px solid rgba(255,255,255,0.1)', borderRadius: '10px', overflow: 'hidden', marginBottom: '2.5rem', backgroundColor: '#0f172a' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.1)', backgroundColor: 'rgba(255,255,255,0.04)' }}>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em' }}>Date</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em' }}>Document Code</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em' }}>Narration / Description</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em' }}>Type</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em', textAlign: 'right' }}>Invoiced Purchase (₹)</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em', textAlign: 'right' }}>Payment Paid (₹)</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: '0.05em', textAlign: 'right' }}>Running AP Balance (₹)</th>
                </tr>
              </thead>
              <tbody>
                {/* Opening Balance Row */}
                <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.06)', backgroundColor: 'rgba(255,255,255,0.02)' }}>
                  <td style={{ padding: '1rem 1.25rem', color: '#cbd5e1', fontWeight: 600 }}>{statement.startDate}</td>
                  <td style={{ padding: '1rem 1.25rem', color: '#64748b', fontWeight: 600, fontFamily: 'monospace' }}>OPEN-BAL</td>
                  <td style={{ padding: '1rem 1.25rem', color: '#f8fafc', fontWeight: 600 }}>Opening Balance Forwarded</td>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <span style={{ padding: '3px 8px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 800, backgroundColor: 'rgba(148, 163, 184, 0.15)', color: '#cbd5e1', letterSpacing: '0.05em' }}>
                      BALANCE
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', color: '#64748b' }}>—</td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', color: '#64748b' }}>—</td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', fontWeight: 800, color: '#f8fafc' }}>
                    ₹ {statement.openingBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </td>
                </tr>

                {/* Map Chronological Transactions */}
                {calculatedEntries.map((e, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.06)', backgroundColor: idx % 2 === 1 ? 'rgba(255,255,255,0.01)' : 'transparent' }}>
                    <td style={{ padding: '1rem 1.25rem', color: '#cbd5e1', whiteSpace: 'nowrap' }}>{e.date}</td>
                    <td style={{ padding: '1rem 1.25rem', color: '#38bdf8', fontWeight: 700, fontFamily: 'monospace' }}>{e.documentCode}</td>
                    <td style={{ padding: '1rem 1.25rem', color: '#f8fafc', fontWeight: 600 }}>{e.formattedDescription}</td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span style={{
                        padding: '3px 8px',
                        borderRadius: '4px',
                        fontSize: '0.7rem',
                        fontWeight: 800,
                        backgroundColor: e.amount > 0 ? 'rgba(251, 191, 36, 0.15)' : 'rgba(52, 211, 153, 0.15)',
                        color: e.amount > 0 ? '#fbbf24' : '#34d399',
                        letterSpacing: '0.05em'
                      }}>
                        {e.amount > 0 ? 'INVOICED' : 'PAYMENT PAID'}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right', fontWeight: 700, color: e.amount > 0 ? '#fbbf24' : '#64748b' }}>
                      {e.amount > 0 ? `₹ ${e.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right', fontWeight: 700, color: e.amount < 0 ? '#34d399' : '#64748b' }}>
                      {e.amount < 0 ? `₹ ${Math.abs(e.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right', fontWeight: 800, color: '#ffffff' }}>
                      ₹ {e.runningBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot style={{ backgroundColor: 'rgba(255,255,255,0.06)', borderTop: '2px solid rgba(255,255,255,0.2)', fontWeight: 800 }}>
                <tr>
                  <td colSpan={4} style={{ padding: '1rem 1.25rem', color: '#ffffff', textAlign: 'right', textTransform: 'uppercase', fontSize: '0.8rem', letterSpacing: '0.05em' }}>
                    Totals:
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', color: '#fbbf24', fontSize: '0.95rem' }}>
                    ₹ {calculatedEntries.filter(e => e.amount > 0).reduce((sum, e) => sum + e.amount, 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', color: '#34d399', fontSize: '0.95rem' }}>
                    ₹ {Math.abs(calculatedEntries.filter(e => e.amount < 0).reduce((sum, e) => sum + e.amount, 0)).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right', color: '#38bdf8', fontSize: '1rem', fontWeight: 800 }}>
                    ₹ {statement.closingBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Reconciliation Footer Card */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: 'rgba(15, 23, 42, 0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '10px', padding: '1.75rem', flexWrap: 'wrap', gap: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              {isReconciled ? (
                <div style={{ width: '42px', height: '42px', borderRadius: '50%', backgroundColor: 'rgba(52, 211, 153, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#34d399' }}>
                  <CheckCircle size={24} />
                </div>
              ) : (
                <div style={{ width: '42px', height: '42px', borderRadius: '50%', backgroundColor: 'rgba(239, 68, 68, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#f87171' }}>
                  <AlertTriangle size={24} />
                </div>
              )}
              <div>
                <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff' }}>
                  {isReconciled ? 'Accounts Payable Reconciled' : 'Audit Discrepancy Detected'}
                </h4>
                <p style={{ color: '#94a3b8', fontSize: '0.82rem', marginTop: '0.2rem' }}>
                  Validates AP balance against sum of unpaid purchase orders minus debit settlements.
                </p>
              </div>
            </div>

            <div style={{ width: '340px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.08)', fontSize: '0.85rem' }}>
                <span style={{ color: '#94a3b8' }}>Total Unpaid Purchase Invoices</span>
                <span style={{ fontWeight: 700, color: '#fbbf24' }}>
                  ₹ {statement.totalUnpaidInvoices.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid rgba(255,255,255,0.08)', fontSize: '0.85rem' }}>
                <span style={{ color: '#94a3b8' }}>Unapplied Settlements / Debits</span>
                <span style={{ fontWeight: 700, color: '#34d399' }}>
                  - ₹ {statement.totalUnappliedDebits.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0 0 0', marginTop: '0.25rem' }}>
                <span style={{ fontWeight: 700, color: '#ffffff' }}>Net Reconciled AP Balance</span>
                <span style={{ fontWeight: 800, fontSize: '1.3rem', color: '#38bdf8' }}>
                  ₹ {statement.reconciledBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          </div>

        </div>
      ) : (
        <div className="glass-panel" style={{ padding: '6rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <FileText size={54} style={{ marginBottom: '1.5rem', color: 'rgba(255,255,255,0.1)' }} />
          <h3 style={{ fontSize: '1.2rem', color: '#fff', fontWeight: 600, marginBottom: '0.5rem' }}>No Statement Generated Yet</h3>
          <p style={{ maxWidth: '440px', margin: '0 auto', fontSize: '0.9rem', lineHeight: '1.4rem' }}>
            Select a supplier vendor and date range above, then click 'Compile Statement' to render the Accounts Payable statement.
          </p>
        </div>
      )}

      {/* Print Styles */}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
        @media print {
          @page {
            size: A4 portrait;
            margin: 15mm 12mm 25mm 12mm;
          }
          body {
            background: #ffffff !important;
            color: #000000 !important;
          }
          .no-print, nav, header, sidebar, button {
            display: none !important;
          }
          .statement-print-area {
            background: #ffffff !important;
            color: #000000 !important;
            border: none !important;
            box-shadow: none !important;
            padding: 0 !important;
          }
          .statement-print-area h2, .statement-print-area h3, .statement-print-area th, .statement-print-area td {
            color: #000000 !important;
          }
          .statement-print-area table {
            border: 1px solid #000000 !important;
          }
          .statement-print-area tr {
            page-break-inside: avoid;
          }
          .statement-print-area tr:nth-last-child(-n+2) {
            page-break-after: avoid;
          }
          .statement-print-area th, .statement-print-area td {
            border: 1px solid #cccccc !important;
          }
        }
      `}} />
    </div>
  );
};

export default SupplierStatement;
