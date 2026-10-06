import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { FundAccountModal } from '../components/funds/FundAccountModal';
import type { FundAccountDto } from '../components/funds/FundAccountModal';
import { FundTransferModal } from '../components/funds/FundTransferModal';
import { CustomerPaymentFundModal } from '../components/funds/CustomerPaymentFundModal';
import { SupplierSettlementFundModal } from '../components/funds/SupplierSettlementFundModal';
import {
  Wallet, Search, Plus, Send, Activity, ChevronLeft, ChevronRight,
  Edit, Trash2, ArrowDownRight, ArrowUpRight, RefreshCw, X, Check
} from 'lucide-react';

interface TransactionSummary {
  totalReceived: number;
  totalSent: number;
}

interface QuickEntryForm {
  type: 'RECEIPT' | 'PAYMENT';
  fundAccountId: string;
  amount: string;
  transactionDate: string;
  description: string;
}

interface FundTransactionDto {
  id: number;
  fundAccountId: number;
  fundAccountName: string;
  fundAccountNumber: string | null;
  targetFundAccountId: number | null;
  targetFundAccountName: string | null;
  type: 'RECEIPT' | 'PAYMENT' | 'TRANSFER';
  amount: number;
  transactionDate: string;
  referenceDocumentType: string | null;
  referenceDocumentId: number | null;
  description: string | null;
}

export const Funds: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('FUND_WRITE');

  const [activeTab, setActiveTab] = useState<'accounts' | 'transactions'>('accounts');

  // Summary
  const [summary, setSummary] = useState<TransactionSummary>({ totalReceived: 0, totalSent: 0 });
  const [summaryLoading, setSummaryLoading] = useState(true);

  // Quick Register
  const [showQuickEntry, setShowQuickEntry] = useState(false);
  const [quickSaving, setQuickSaving] = useState(false);
  const [quickForm, setQuickForm] = useState<QuickEntryForm>({
    type: 'RECEIPT',
    fundAccountId: '',
    amount: '',
    transactionDate: new Date().toISOString().split('T')[0],
    description: ''
  });

  // Accounts State
  const [accounts, setAccounts] = useState<FundAccountDto[]>([]);
  const [accLoading, setAccLoading] = useState(true);
  const [accPage, setAccPage] = useState(0);
  const [accSize] = useState(10);
  const [accTotalPages, setAccTotalPages] = useState(0);
  const [accTotalElements, setAccTotalElements] = useState(0);
  const [accSearch, setAccSearch] = useState('');
  const [accTypeFilter, setAccTypeFilter] = useState('');
  const [accActiveFilter, setAccActiveFilter] = useState('true');

  const [isAccModalOpen, setIsAccModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<FundAccountDto | null>(null);

  // Transactions State
  const [transactions, setTransactions] = useState<FundTransactionDto[]>([]);
  const [txLoading, setTxLoading] = useState(true);
  const [txPage, setTxPage] = useState(0);
  const [txSize] = useState(10);
  const [txTotalPages, setTxTotalPages] = useState(0);
  const [txTotalElements, setTxTotalElements] = useState(0);
  const [txTypeFilter, setTxTypeFilter] = useState('');

  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false);
  const [isSupplierModalOpen, setIsSupplierModalOpen] = useState(false);

  useEffect(() => {
    fetchSummary();
    fetchAccounts();
  }, []);

  useEffect(() => {
    if (activeTab === 'accounts') fetchAccounts();
    else if (activeTab === 'transactions') fetchTransactions();
  }, [activeTab, accPage, accSearch, accTypeFilter, accActiveFilter, txPage, txTypeFilter]);

  const fetchSummary = async () => {
    setSummaryLoading(true);
    try {
      const res = await api.get('/api/funds/transactions/summary');
      setSummary({
        totalReceived: res.data.totalReceived ?? 0,
        totalSent: res.data.totalSent ?? 0,
      });
    } catch (err) {
      console.error('Failed to fetch transaction summary', err);
    } finally {
      setSummaryLoading(false);
    }
  };

  const refreshAll = () => {
    fetchSummary();
    fetchAccounts();
    if (activeTab === 'transactions') fetchTransactions();
  };

  const fmt = (val: number) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(val || 0);

  const handleQuickSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!quickForm.fundAccountId || !quickForm.amount) {
      toastEvents.error('Please select an account and enter an amount');
      return;
    }
    const amount = parseFloat(quickForm.amount);
    if (isNaN(amount) || amount <= 0) {
      toastEvents.error('Please enter a valid amount greater than zero');
      return;
    }
    setQuickSaving(true);
    try {
      await api.post('/api/funds/transactions/record', {
        fundAccountId: parseInt(quickForm.fundAccountId),
        type: quickForm.type,
        amount,
        transactionDate: quickForm.transactionDate || new Date().toISOString().split('T')[0],
        description: quickForm.description || (quickForm.type === 'RECEIPT' ? 'Money received' : 'Money sent'),
      });
      toastEvents.success(quickForm.type === 'RECEIPT' ? 'Money received registered' : 'Money sent registered');
      setQuickForm(f => ({ ...f, amount: '', description: '' }));
      setShowQuickEntry(false);
      refreshAll();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to register transaction');
    } finally {
      setQuickSaving(false);
    }
  };

  const fetchAccounts = async () => {
    setAccLoading(true);
    try {
      const params: any = {};
      if (accSearch) params.search = accSearch;
      if (accTypeFilter) params.type = accTypeFilter;
      if (accActiveFilter !== '') params.active = accActiveFilter;

      const data = await fetchPaginated<FundAccountDto>('/api/funds/accounts', { page: accPage, size: accSize, sort: 'id,desc', ...params });
      setAccounts(data.content);
      setAccTotalPages(data.totalPages);
      setAccTotalElements(data.totalElements);
    } catch (err) {
      toastEvents.error('Failed to load fund accounts');
    } finally {
      setAccLoading(false);
    }
  };

  const fetchTransactions = async () => {
    setTxLoading(true);
    try {
      const params: any = {};
      if (txTypeFilter) params.type = txTypeFilter;

      const data = await fetchPaginated<FundTransactionDto>('/api/funds/transactions', { page: txPage, size: txSize, sort: 'transactionDate,desc', ...params });
      setTransactions(data.content);
      setTxTotalPages(data.totalPages);
      setTxTotalElements(data.totalElements);
    } catch (err) {
      toastEvents.error('Failed to load transactions');
    } finally {
      setTxLoading(false);
    }
  };

  const handleDeactivateAccount = async (id: number) => {
    if (!window.confirm('Are you sure you want to deactivate this account?')) return;
    try {
      await api.delete(`/api/funds/accounts/${id}`);
      toastEvents.success('Account deactivated');
      refreshAll();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to deactivate account');
    }
  };

  const handleReverseTx = async (id: number) => {
    if (!window.confirm('Are you sure you want to reverse this transaction?')) return;
    try {
      await api.post(`/api/funds/transactions/${id}/reverse`);
      toastEvents.success('Transaction reversed successfully');
      fetchTransactions();
      fetchSummary();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to reverse transaction');
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Wallet size={22} />
          </div>
          <div>
            <h1 className="page-title">Funds Management</h1>
            <p className="page-description">Manage bank accounts and cash funds.</p>
          </div>
        </div>
        <div className="header-actions" style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ background: 'linear-gradient(135deg,rgba(16,185,129,0.12),rgba(16,185,129,0.05))', border: '1px solid rgba(16,185,129,0.3)', borderRadius: '12px', padding: '0.7rem 1.1rem', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', minWidth: '150px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.15rem' }}>
              <ArrowDownRight size={13} color="#10b981" />
              <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: '#10b981' }}>Total Received</span>
            </div>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#10b981', fontFamily: 'monospace', lineHeight: 1.2 }}>
              {summaryLoading ? '...' : fmt(summary.totalReceived)}
            </span>
          </div>
          <div style={{ background: 'linear-gradient(135deg,rgba(239,68,68,0.12),rgba(239,68,68,0.05))', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '12px', padding: '0.7rem 1.1rem', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', minWidth: '150px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.15rem' }}>
              <ArrowUpRight size={13} color="#ef4444" />
              <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: '#ef4444' }}>Total Sent</span>
            </div>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ef4444', fontFamily: 'monospace', lineHeight: 1.2 }}>
              {summaryLoading ? '...' : fmt(summary.totalSent)}
            </span>
          </div>
          <button onClick={refreshAll} title="Refresh" style={{ padding: '0.5rem', borderRadius: '8px', border: '1px solid var(--border)', background: 'var(--surface)', cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
            <RefreshCw size={16} color="var(--text-secondary)" />
          </button>
        </div>
      </div>

      {/* Quick Register Panel */}
      {hasWriteAccess && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', marginBottom: '1rem', overflow: 'hidden' }}>
          {!showQuickEntry ? (
            <div style={{ display: 'flex', gap: '0.75rem', padding: '0.875rem 1.25rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginRight: '0.25rem' }}>Quick Register:</span>
              <button onClick={() => { setQuickForm(f => ({ ...f, type: 'RECEIPT', fundAccountId: accounts[0]?.id?.toString() || '' })); setShowQuickEntry(true); }} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.5rem 1rem', borderRadius: '8px', fontWeight: 700, fontSize: '0.85rem', background: 'rgba(16,185,129,0.12)', color: '#10b981', border: '1.5px solid rgba(16,185,129,0.4)', cursor: 'pointer' }}>
                <ArrowDownRight size={15} /> Money In
              </button>
              <button onClick={() => { setQuickForm(f => ({ ...f, type: 'PAYMENT', fundAccountId: accounts[0]?.id?.toString() || '' })); setShowQuickEntry(true); }} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.5rem 1rem', borderRadius: '8px', fontWeight: 700, fontSize: '0.85rem', background: 'rgba(239,68,68,0.1)', color: '#ef4444', border: '1.5px solid rgba(239,68,68,0.35)', cursor: 'pointer' }}>
                <ArrowUpRight size={15} /> Money Out
              </button>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.5rem' }}>or use the Transactions tab for advanced options</span>
            </div>
          ) : (
            <form onSubmit={handleQuickSubmit}>
              <div style={{ background: quickForm.type === 'RECEIPT' ? 'linear-gradient(135deg,rgba(16,185,129,0.08),transparent)' : 'linear-gradient(135deg,rgba(239,68,68,0.08),transparent)', padding: '1rem 1.25rem', borderBottom: `2px solid ${quickForm.type === 'RECEIPT' ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}` }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.875rem', flexWrap: 'wrap' }}>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button type="button" onClick={() => setQuickForm(f => ({ ...f, type: 'RECEIPT' }))} style={{ padding: '0.4rem 0.9rem', borderRadius: '7px', fontWeight: 700, fontSize: '0.8rem', cursor: 'pointer', background: quickForm.type === 'RECEIPT' ? '#10b981' : 'transparent', color: quickForm.type === 'RECEIPT' ? '#fff' : '#10b981', border: '1.5px solid rgba(16,185,129,0.5)' }}>
                      <ArrowDownRight size={13} style={{ display: 'inline', marginRight: '3px', verticalAlign: 'middle' }} /> Money In
                    </button>
                    <button type="button" onClick={() => setQuickForm(f => ({ ...f, type: 'PAYMENT' }))} style={{ padding: '0.4rem 0.9rem', borderRadius: '7px', fontWeight: 700, fontSize: '0.8rem', cursor: 'pointer', background: quickForm.type === 'PAYMENT' ? '#ef4444' : 'transparent', color: quickForm.type === 'PAYMENT' ? '#fff' : '#ef4444', border: '1.5px solid rgba(239,68,68,0.5)' }}>
                      <ArrowUpRight size={13} style={{ display: 'inline', marginRight: '3px', verticalAlign: 'middle' }} /> Money Out
                    </button>
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{quickForm.type === 'RECEIPT' ? 'Register money received into account' : 'Register money sent from account'}</span>
                  <button type="button" onClick={() => setShowQuickEntry(false)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: '2px' }}><X size={18} /></button>
                </div>
                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', minWidth: '170px' }}>
                    <label style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Account *</label>
                    <select className="form-select" value={quickForm.fundAccountId} onChange={e => setQuickForm(f => ({ ...f, fundAccountId: e.target.value }))} required style={{ fontSize: '0.85rem', padding: '0.45rem 0.75rem' }}>
                      <option value="">Select account...</option>
                      {accounts.filter(a => a.active).map(a => (
                        <option key={a.id} value={a.id}>{a.name} ({a.type})</option>
                      ))}
                    </select>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', minWidth: '120px' }}>
                    <label style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Amount (₹) *</label>
                    <input type="number" min="0.01" step="0.01" placeholder="0.00" value={quickForm.amount} onChange={e => setQuickForm(f => ({ ...f, amount: e.target.value }))} required style={{ fontSize: '0.9rem', fontWeight: 700, padding: '0.45rem 0.75rem', border: '1px solid var(--border)', borderRadius: '7px', background: 'var(--surface)', color: 'var(--text-primary)', width: '100%' }} />
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', minWidth: '135px' }}>
                    <label style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Date</label>
                    <input type="date" value={quickForm.transactionDate} onChange={e => setQuickForm(f => ({ ...f, transactionDate: e.target.value }))} style={{ fontSize: '0.85rem', padding: '0.45rem 0.75rem', border: '1px solid var(--border)', borderRadius: '7px', background: 'var(--surface)', color: 'var(--text-primary)' }} />
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', flex: 1, minWidth: '160px' }}>
                    <label style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Description</label>
                    <input type="text" placeholder={quickForm.type === 'RECEIPT' ? 'e.g. Customer payment' : 'e.g. Vendor payment'} value={quickForm.description} onChange={e => setQuickForm(f => ({ ...f, description: e.target.value }))} style={{ fontSize: '0.85rem', padding: '0.45rem 0.75rem', border: '1px solid var(--border)', borderRadius: '7px', background: 'var(--surface)', color: 'var(--text-primary)' }} />
                  </div>
                  <button type="submit" disabled={quickSaving} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.5rem 1.2rem', borderRadius: '8px', fontWeight: 700, fontSize: '0.85rem', background: quickForm.type === 'RECEIPT' ? '#10b981' : '#ef4444', color: '#fff', border: 'none', cursor: quickSaving ? 'not-allowed' : 'pointer', opacity: quickSaving ? 0.7 : 1, whiteSpace: 'nowrap' }}>
                    <Check size={15} /> {quickSaving ? 'Saving...' : 'Register'}
                  </button>
                </div>
              </div>
            </form>
          )}
        </div>
      )}

      <div className="tabs">
        <button className={`tab ${activeTab === 'accounts' ? 'active' : ''}`} onClick={() => { setActiveTab('accounts'); setAccPage(0); }}>
          <Wallet size={18} />
          <span>Accounts</span>
        </button>
        <button className={`tab ${activeTab === 'transactions' ? 'active' : ''}`} onClick={() => { setActiveTab('transactions'); setTxPage(0); fetchTransactions(); }}>
          <Activity size={18} />
          <span>Transaction History</span>
        </button>
      </div>

      {activeTab === 'accounts' && (
        <>
          <div className="filters-panel">
            <div className="filters-group">
              <div className="search-input-wrapper">
                <Search size={16} className="search-icon" />
                <input
                  type="text"
                  className="search-input"
                  placeholder="Search name or account no..."
                  value={accSearch}
                  onChange={(e) => { setAccSearch(e.target.value); setAccPage(0); }}
                />
              </div>
              <select className="form-select" value={accTypeFilter} onChange={(e) => { setAccTypeFilter(e.target.value); setAccPage(0); }} style={{ width: 'auto', minWidth: '110px' }}>
                <option value="">All Types</option>
                <option value="CASH">Cash</option>
                <option value="BANK">Bank</option>
              </select>
              <select className="form-select" value={accActiveFilter} onChange={(e) => { setAccActiveFilter(e.target.value); setAccPage(0); }} style={{ width: 'auto', minWidth: '120px' }}>
                <option value="">All Statuses</option>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
            {hasWriteAccess && (
              <div className="action-btn-group">
                <button className="btn btn-primary" onClick={() => { setEditingAccount(null); setIsAccModalOpen(true); }}>
                  <Plus size={16} /> Add Account
                </button>
              </div>
            )}
          </div>

          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr className="table-header-row">
                  <th>ID</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Account No.</th>
                  <th>Status</th>
                  {hasWriteAccess && <th style={{ textAlign: 'right' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {accLoading ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} style={{ textAlign: 'center', padding: '2rem' }}>Loading accounts...</td></tr>
                ) : accounts.length === 0 ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} style={{ textAlign: 'center', padding: '2rem' }}>No accounts found</td></tr>
                ) : accounts.map(acc => (
                  <tr key={acc.id} className="table-row">
                    <td>#{acc.id}</td>
                    <td style={{ fontWeight: 600 }}>{acc.name}</td>
                    <td>
                      <span style={{ display: 'inline-flex', alignItems: 'center', padding: '0.25rem 0.75rem', borderRadius: '6px', fontSize: '0.72rem', fontWeight: 700, backgroundColor: acc.type === 'BANK' ? 'rgba(59, 130, 246, 0.15)' : 'rgba(255,255,255,0.1)', color: acc.type === 'BANK' ? '#3b82f6' : 'var(--text-secondary)', letterSpacing: '0.02em', textTransform: 'uppercase' }}>
                        {acc.type}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'monospace' }}>{acc.accountNumber || '-'}</td>
                    <td>
                      <span className={`badge ${acc.active ? 'badge-success' : 'badge-danger'}`}>
                        {acc.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    {hasWriteAccess && (
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                          <button className="btn btn-secondary btn-sm" title="Edit" onClick={() => { setEditingAccount(acc); setIsAccModalOpen(true); }}>
                            <Edit size={14} />
                          </button>
                          {acc.active && (
                            <button className="btn btn-danger btn-sm" title="Deactivate" onClick={() => handleDeactivateAccount(acc.id)}>
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {accTotalPages > 1 && (
            <div className="pagination-panel">
              <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Showing {accPage * accSize + 1} to {Math.min((accPage + 1) * accSize, accTotalElements)} of {accTotalElements} records</span>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <button className="btn btn-secondary" onClick={() => setAccPage(p => Math.max(0, p - 1))} disabled={accPage === 0} style={{ opacity: accPage === 0 ? 0.4 : 1, cursor: accPage === 0 ? 'not-allowed' : 'pointer' }}><ChevronLeft size={16} /></button>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>Page {accPage + 1} of {accTotalPages}</span>
                <button className="btn btn-secondary" onClick={() => setAccPage(p => Math.min(accTotalPages - 1, p + 1))} disabled={accPage === accTotalPages - 1} style={{ opacity: accPage === accTotalPages - 1 ? 0.4 : 1, cursor: accPage === accTotalPages - 1 ? 'not-allowed' : 'pointer' }}><ChevronRight size={16} /></button>
              </div>
            </div>
          )}
        </>
      )}

      {activeTab === 'transactions' && (
        <>
          <div className="filters-panel">
            <div className="filters-group">
              <select className="form-select" value={txTypeFilter} onChange={(e) => { setTxTypeFilter(e.target.value); setTxPage(0); }} style={{ width: 'auto', minWidth: '170px' }}>
                <option value="">All Transactions</option>
                <option value="RECEIPT">Money In (Receipts)</option>
                <option value="PAYMENT">Money Out (Payments)</option>
                <option value="TRANSFER">Transfers</option>
              </select>
            </div>
            {hasWriteAccess && (
              <div className="action-btn-group">
                <button className="btn btn-success" style={{ backgroundColor: '#10b981', borderColor: '#10b981', color: '#fff' }} onClick={() => setIsCustomerModalOpen(true)}>
                  <ArrowDownRight size={16} /> Customer Payment In
                </button>
                <button className="btn btn-danger" style={{ backgroundColor: '#ef4444', borderColor: '#ef4444', color: '#fff' }} onClick={() => setIsSupplierModalOpen(true)}>
                  <ArrowUpRight size={16} /> Supplier Settle Out
                </button>
                <button className="btn btn-primary" onClick={() => setIsTransferModalOpen(true)}>
                  <Send size={16} /> Transfer
                </button>
              </div>
            )}
          </div>

          {/* Totals mini-banner */}
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '0.75rem', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 0.9rem', borderRadius: '8px', background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.2)', fontSize: '0.82rem', fontWeight: 700, color: '#10b981' }}>
              <ArrowDownRight size={14} /> Total Received: {summaryLoading ? '...' : fmt(summary.totalReceived)}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 0.9rem', borderRadius: '8px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.2)', fontSize: '0.82rem', fontWeight: 700, color: '#ef4444' }}>
              <ArrowUpRight size={14} /> Total Sent: {summaryLoading ? '...' : fmt(summary.totalSent)}
            </div>
          </div>

          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr className="table-header-row">
                  <th>Date</th>
                  <th>Type</th>
                  <th>Account</th>
                  <th>Description</th>
                  <th style={{ textAlign: 'right' }}>Amount</th>
                  {hasWriteAccess && <th className="text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {txLoading ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} className="text-center py-4">Loading...</td></tr>
                ) : transactions.length === 0 ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} className="text-center py-4">No transactions found</td></tr>
                ) : transactions.map(tx => (
                  <tr key={tx.id} className="table-row">
                    <td style={{ fontFamily: 'monospace', fontSize: '0.82rem' }}>{tx.transactionDate}</td>
                    <td>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.25rem 0.65rem', borderRadius: '6px', fontSize: '0.72rem', fontWeight: 700, backgroundColor: tx.type === 'RECEIPT' ? 'rgba(16,185,129,0.15)' : tx.type === 'PAYMENT' ? 'rgba(239,68,68,0.15)' : 'rgba(245,158,11,0.15)', color: tx.type === 'RECEIPT' ? '#10b981' : tx.type === 'PAYMENT' ? '#ef4444' : '#f59e0b', letterSpacing: '0.02em', textTransform: 'uppercase' }}>
                        {tx.type === 'RECEIPT' ? <ArrowDownRight size={11} /> : tx.type === 'PAYMENT' ? <ArrowUpRight size={11} /> : <Send size={11} />}
                        {tx.type === 'RECEIPT' ? 'Money In' : tx.type === 'PAYMENT' ? 'Money Out' : 'Transfer'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.85rem' }}>
                      {tx.type === 'TRANSFER'
                        ? <span>{tx.fundAccountName} <span style={{ color: 'var(--text-muted)' }}>to</span> {tx.targetFundAccountName}</span>
                        : tx.fundAccountName}
                    </td>
                    <td style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', maxWidth: '220px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={tx.description || ''}>{tx.description || '-'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 700, fontFamily: 'monospace', color: tx.type === 'RECEIPT' ? '#10b981' : tx.type === 'PAYMENT' ? '#ef4444' : 'var(--text-primary)' }}>
                      {tx.type === 'RECEIPT' ? '+' : tx.type === 'PAYMENT' ? '-' : ''}{fmt(tx.amount)}
                    </td>
                    {hasWriteAccess && (
                      <td className="text-right">
                        <div className="action-buttons justify-end">
                          {tx.referenceDocumentType !== 'REVERSAL' && (
                            <button className="icon-button" title="Reverse Transaction" onClick={() => handleReverseTx(tx.id)}>
                              <Trash2 size={16} className="text-red-600" />
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {txTotalPages > 1 && (
            <div className="pagination-panel">
              <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Showing {txPage * txSize + 1} to {Math.min((txPage + 1) * txSize, txTotalElements)} of {txTotalElements} records</span>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <button className="btn btn-secondary" onClick={() => setTxPage(p => Math.max(0, p - 1))} disabled={txPage === 0} style={{ opacity: txPage === 0 ? 0.4 : 1, cursor: txPage === 0 ? 'not-allowed' : 'pointer' }}><ChevronLeft size={16} /></button>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>Page {txPage + 1} of {txTotalPages}</span>
                <button className="btn btn-secondary" onClick={() => setTxPage(p => Math.min(txTotalPages - 1, p + 1))} disabled={txPage === txTotalPages - 1} style={{ opacity: txPage === txTotalPages - 1 ? 0.4 : 1, cursor: txPage === txTotalPages - 1 ? 'not-allowed' : 'pointer' }}><ChevronRight size={16} /></button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Modals */}
      <FundAccountModal
        isOpen={isAccModalOpen}
        onClose={() => setIsAccModalOpen(false)}
        account={editingAccount}
        onSaved={() => { fetchAccounts(); fetchSummary(); }}
      />
      <FundTransferModal
        isOpen={isTransferModalOpen}
        onClose={() => setIsTransferModalOpen(false)}
        accounts={accounts}
        onSaved={() => { fetchTransactions(); fetchAccounts(); fetchSummary(); }}
      />
      <CustomerPaymentFundModal
        isOpen={isCustomerModalOpen}
        onClose={() => setIsCustomerModalOpen(false)}
        accounts={accounts}
        onSaved={() => { fetchTransactions(); fetchAccounts(); fetchSummary(); }}
      />
      <SupplierSettlementFundModal
        isOpen={isSupplierModalOpen}
        onClose={() => setIsSupplierModalOpen(false)}
        accounts={accounts}
        onSaved={() => { fetchTransactions(); fetchAccounts(); fetchSummary(); }}
      />
    </div>
  );
};
