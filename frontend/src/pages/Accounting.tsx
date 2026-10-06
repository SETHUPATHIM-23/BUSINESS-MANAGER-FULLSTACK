import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { toastEvents } from '../utils/toast';
import { useAuth } from '../context/AuthContext';
import { 
  Plus, Search, Edit, Trash2, Eye, ChevronLeft, ChevronRight, 
  ArrowUpDown, SlidersHorizontal, AlertCircle, RefreshCw, X, 
  Lock, Unlock, Calendar, DollarSign, CheckSquare, ListFilter
} from 'lucide-react';

interface AccountDto {
  id: number;
  code: string;
  name: string;
  type: string;
  parentId: number | null;
  parentCode: string | null;
  parentName: string | null;
  active: boolean;
  debitTotal: number;
  creditTotal: number;
  netBalance: number;
}

interface JournalEntrySummary {
  id: number;
  entryDate: string;
  reference: string;
  memo: string;
  sourceModule: string;
  sourceDocumentId: number | null;
  totalAmount: number;
}

interface JournalLineDto {
  id: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  description: string;
}

interface JournalEntryDetail {
  id: number;
  entryDate: string;
  reference: string;
  memo: string;
  sourceModule: string;
  sourceDocumentId: number | null;
  fiscalPeriodId: number;
  fiscalPeriodName: string;
  lines: JournalLineDto[];
  totalDebit: number;
  totalCredit: number;
}

interface FiscalPeriodDto {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  closed: boolean;
}

export const Accounting: React.FC = () => {
  const { hasPermission, permissions } = useAuth();
  const hasWriteAccess = hasPermission('ACCOUNTING_WRITE');
  const isAdmin = permissions.includes('ROLE_ADMINISTRATOR');

  const [activeTab, setActiveTab] = useState<'accounts' | 'entries' | 'periods'>('accounts');

  // --- ACCOUNTS STATE ---
  const [accounts, setAccounts] = useState<AccountDto[]>([]);
  const [accountsLoading, setAccountsLoading] = useState(true);
  const [accountSearch, setAccountSearch] = useState('');
  const [accountTypeFilter, setAccountTypeFilter] = useState('');
  const [accountPage, setAccountPage] = useState(0);
  const [accountPageSize] = useState(10);
  const [accountTotalPages, setAccountTotalPages] = useState(0);
  const [accountTotalElements, setAccountTotalElements] = useState(0);
  const [accountSortBy, setAccountSortBy] = useState('code');
  const [accountSortDir, setAccountSortDir] = useState<'asc' | 'desc'>('asc');

  // --- JOURNAL ENTRIES STATE ---
  const [entries, setEntries] = useState<JournalEntrySummary[]>([]);
  const [entriesLoading, setEntriesLoading] = useState(true);
  const [entrySearch, setEntrySearch] = useState('');
  const [entryStartDate, setEntryStartDate] = useState('');
  const [entryEndDate, setEntryEndDate] = useState('');
  const [entrySourceModule, setEntrySourceModule] = useState('');
  const [entryPage, setEntryPage] = useState(0);
  const [entryPageSize] = useState(10);
  const [entryTotalPages, setEntryTotalPages] = useState(0);
  const [entryTotalElements, setEntryTotalElements] = useState(0);
  const [entrySortBy, setEntrySortBy] = useState('entryDate');
  const [entrySortDir, setEntrySortDir] = useState<'asc' | 'desc'>('desc');

  // --- FISCAL PERIODS STATE ---
  const [periods, setPeriods] = useState<FiscalPeriodDto[]>([]);
  const [periodsLoading, setPeriodsLoading] = useState(true);

  // --- SAVING INDICATORS ---
  const [accountSaving, setAccountSaving] = useState(false);
  const [entryPosting, setEntryPosting] = useState(false);
  const [periodSaving, setPeriodSaving] = useState(false);

  // --- INLINE VALIDATION ERROR STATES ---
  const [accountErrors, setAccountErrors] = useState<Record<string, string>>({});
  const [entryErrors, setEntryErrors] = useState<Record<string, string>>({});
  const [periodErrors, setPeriodErrors] = useState<Record<string, string>>({});

  // --- MODALS STATE ---
  const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<AccountDto | null>(null);
  const [accountForm, setAccountForm] = useState({
    code: '',
    name: '',
    type: 'ASSET',
    parentId: '',
    active: true
  });

  const [isEntryDetailModalOpen, setIsEntryDetailModalOpen] = useState(false);
  const [viewingEntry, setViewingEntry] = useState<JournalEntryDetail | null>(null);

  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [postForm, setPostForm] = useState({
    entryDate: new Date().toISOString().split('T')[0],
    reference: '',
    memo: '',
    sourceModule: 'MANUAL',
    lines: [
      { accountId: '', debitAmount: 0, creditAmount: 0, description: '' },
      { accountId: '', debitAmount: 0, creditAmount: 0, description: '' }
    ]
  });

  const [isPeriodModalOpen, setIsPeriodModalOpen] = useState(false);
  const [periodForm, setPeriodForm] = useState({
    name: '',
    startDate: '',
    endDate: '',
    closed: false
  });

  // --- DATA FETCHING ---

  const fetchAccounts = async () => {
    try {
      setAccountsLoading(true);
      const params: Record<string, any> = {
        page: accountPage,
        size: accountPageSize,
        sortBy: accountSortBy,
        direction: accountSortDir
      };
      if (accountSearch) params.search = accountSearch;
      if (accountTypeFilter) params.type = accountTypeFilter;

      const response = await fetchPaginated<AccountDto>('/api/accountings/accounts', params);
      setAccounts(response.content);
      setAccountTotalPages(response.totalPages);
      setAccountTotalElements(response.totalElements);
    } catch (err) {
      toastEvents.error('Failed to load accounts');
    } finally {
      setAccountsLoading(false);
    }
  };

  const fetchEntries = async () => {
    try {
      setEntriesLoading(true);
      const params: Record<string, any> = {
        page: entryPage,
        size: entryPageSize,
        sortBy: entrySortBy,
        direction: entrySortDir
      };
      if (entrySearch) params.search = entrySearch;
      if (entryStartDate) params.startDate = entryStartDate;
      if (entryEndDate) params.endDate = entryEndDate;
      if (entrySourceModule) params.sourceModule = entrySourceModule;

      const response = await fetchPaginated<JournalEntrySummary>('/api/accountings/entries', params);
      setEntries(response.content);
      setEntryTotalPages(response.totalPages);
      setEntryTotalElements(response.totalElements);
    } catch (err) {
      toastEvents.error('Failed to load journal entries');
    } finally {
      setEntriesLoading(false);
    }
  };

  const fetchPeriods = async () => {
    try {
      setPeriodsLoading(true);
      const response = await api.get<FiscalPeriodDto[]>('/api/accountings/periods');
      setPeriods(response.data);
    } catch (err) {
      toastEvents.error('Failed to load fiscal periods');
    } finally {
      setPeriodsLoading(false);
    }
  };

  // Trigger loads based on active tab & filters
  useEffect(() => {
    if (activeTab === 'accounts') fetchAccounts();
    if (activeTab === 'entries') fetchEntries();
    if (activeTab === 'periods') fetchPeriods();
  }, [activeTab, accountPage, accountPageSize, accountSortBy, accountSortDir, accountTypeFilter, entryPage, entryPageSize, entrySortBy, entrySortDir, entrySourceModule]);

  // Handle account filters search trigger
  const handleAccountSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setAccountPage(0);
    fetchAccounts();
  };

  const handleEntrySearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setEntryPage(0);
    fetchEntries();
  };

  // --- ACTIONS ---

  const handleDeactivateAccount = async (id: number, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await api.post(`/api/accountings/accounts/${id}/deactivate`);
        toastEvents.success('Account deactivated successfully');
      } else {
        // Reactivate can be done by editing status to active = true
        const acc = accounts.find(a => a.id === id);
        if (acc) {
          await api.put(`/api/accountings/accounts/${id}`, {
            ...acc,
            active: true
          });
          toastEvents.success('Account reactivated successfully');
        }
      }
      fetchAccounts();
    } catch (err) {
      toastEvents.error('Failed to update account status');
    }
  };

  const handleDeleteAccount = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this account?')) return;
    try {
      await api.delete(`/api/accountings/accounts/${id}`);
      toastEvents.success('Account deleted successfully');
      fetchAccounts();
    } catch (err) {
      // Handled by API interceptor
    }
  };

  const handleOpenEditAccount = (acc: AccountDto) => {
    setEditingAccount(acc);
    setAccountForm({
      code: acc.code,
      name: acc.name,
      type: acc.type,
      parentId: acc.parentId ? acc.parentId.toString() : '',
      active: acc.active
    });
    setAccountErrors({});
    setIsAccountModalOpen(true);
  };

  const handleOpenCreateAccount = () => {
    setEditingAccount(null);
    setAccountForm({
      code: '',
      name: '',
      type: 'ASSET',
      parentId: '',
      active: true
    });
    setAccountErrors({});
    setIsAccountModalOpen(true);
  };

  const validateAccountClientSide = (): boolean => {
    const errs: Record<string, string> = {};
    if (!accountForm.code || accountForm.code.trim() === '') {
      errs.code = 'Account code is required.';
    } else if (accountForm.code.length > 20) {
      errs.code = 'Code cannot exceed 20 characters.';
    }
    if (!accountForm.name || accountForm.name.trim() === '') {
      errs.name = 'Account name is required.';
    } else if (accountForm.name.length > 100) {
      errs.name = 'Name cannot exceed 100 characters.';
    }
    setAccountErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSaveAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateAccountClientSide()) return;

    setAccountSaving(true);
    try {
      const payload = {
        code: accountForm.code,
        name: accountForm.name,
        type: accountForm.type,
        parentId: accountForm.parentId ? parseInt(accountForm.parentId) : null,
        active: accountForm.active
      };

      if (editingAccount) {
        await api.put(`/api/accountings/accounts/${editingAccount.id}`, payload);
        toastEvents.success('Account updated successfully');
      } else {
        await api.post('/api/accountings/accounts', payload);
        toastEvents.success('Account created successfully');
      }
      setIsAccountModalOpen(false);
      fetchAccounts();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const errorMap: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((f: any) => {
          errorMap[f.field] = f.message;
        });
        setAccountErrors(errorMap);
      }
    } finally {
      setAccountSaving(false);
    }
  };

  const handleViewEntry = async (id: number) => {
    try {
      const response = await api.get<JournalEntryDetail>(`/api/accountings/entries/${id}`);
      setViewingEntry(response.data);
      setIsEntryDetailModalOpen(true);
    } catch (err) {
      toastEvents.error('Failed to view journal entry details');
    }
  };

  const handleAddPostLine = () => {
    setPostForm({
      ...postForm,
      lines: [...postForm.lines, { accountId: '', debitAmount: 0, creditAmount: 0, description: '' }]
    });
  };

  const handleRemovePostLine = (index: number) => {
    if (postForm.lines.length <= 2) {
      toastEvents.error('A journal entry must contain at least 2 lines.');
      return;
    }
    const newLines = [...postForm.lines];
    newLines.splice(index, 1);
    setPostForm({ ...postForm, lines: newLines });
  };

  const handleLineChange = (index: number, field: string, val: any) => {
    const newLines = [...postForm.lines] as any[];
    newLines[index][field] = val;
    setPostForm({ ...postForm, lines: newLines });
  };

  const validateJournalEntryClientSide = (): boolean => {
    const errs: Record<string, string> = {};
    if (!postForm.reference || postForm.reference.trim() === '') {
      errs.reference = 'Document reference is required.';
    } else if (postForm.reference.length > 100) {
      errs.reference = 'Reference cannot exceed 100 characters.';
    }

    if (postForm.memo && postForm.memo.length > 255) {
      errs.memo = 'Memo cannot exceed 255 characters.';
    }

    // Lines validation
    const lineErrors: string[] = [];
    let isLinesValid = true;
    postForm.lines.forEach((l, idx) => {
      if (!l.accountId) {
        lineErrors[idx] = 'Account is required.';
        isLinesValid = false;
      }
      if (l.debitAmount < 0 || l.creditAmount < 0) {
        lineErrors[idx] = 'Amounts cannot be negative.';
        isLinesValid = false;
      }
    });

    if (!isLinesValid) {
      errs.lines = 'Please fix individual line item errors.';
    }

    const debits = postForm.lines.reduce((sum, l) => sum + parseFloat(l.debitAmount as any || 0), 0);
    const credits = postForm.lines.reduce((sum, l) => sum + parseFloat(l.creditAmount as any || 0), 0);

    if (debits !== credits) {
      errs.balance = `Double-entry unbalanced. Total Debits: ${debits.toFixed(2)}, Total Credits: ${credits.toFixed(2)}`;
    } else if (debits === 0) {
      errs.balance = 'Transaction total amount must be greater than ₹0.00.';
    }

    setEntryErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handlePostJournalEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateJournalEntryClientSide()) return;

    setEntryPosting(true);
    try {
      const payload = {
        entryDate: postForm.entryDate,
        reference: postForm.reference,
        memo: postForm.memo,
        sourceModule: postForm.sourceModule,
        lines: postForm.lines.map(l => ({
          accountId: parseInt(l.accountId),
          debitAmount: parseFloat(l.debitAmount as any || 0),
          creditAmount: parseFloat(l.creditAmount as any || 0),
          description: l.description
        }))
      };

      await api.post('/api/accountings/entries', payload);
      toastEvents.success('Journal entry posted successfully');
      setIsPostModalOpen(false);
      
      // Reset form
      setPostForm({
        entryDate: new Date().toISOString().split('T')[0],
        reference: '',
        memo: '',
        sourceModule: 'MANUAL',
        lines: [
          { accountId: '', debitAmount: 0, creditAmount: 0, description: '' },
          { accountId: '', debitAmount: 0, creditAmount: 0, description: '' }
        ]
      });
      setEntryErrors({});
      fetchEntries();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const errorMap: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((f: any) => {
          errorMap[f.field] = f.message;
        });
        setEntryErrors(errorMap);
      }
    } finally {
      setEntryPosting(false);
    }
  };

  const handleClosePeriod = async (id: number) => {
    if (!window.confirm('Are you sure you want to CLOSE this fiscal period? Posting will be locked.')) return;
    try {
      await api.post(`/api/accountings/periods/${id}/close`);
      toastEvents.success('Fiscal period locked successfully');
      fetchPeriods();
    } catch (err) {
      toastEvents.error('Failed to close fiscal period');
    }
  };

  const handleReopenPeriod = async (id: number) => {
    if (!window.confirm('Are you sure you want to REOPEN this fiscal period?')) return;
    try {
      await api.post(`/api/accountings/periods/${id}/reopen`);
      toastEvents.success('Fiscal period reopened successfully');
      fetchPeriods();
    } catch (err) {
      toastEvents.error('Failed to reopen fiscal period');
    }
  };

  const validatePeriodClientSide = (): boolean => {
    const errs: Record<string, string> = {};
    if (!periodForm.name || periodForm.name.trim() === '') {
      errs.name = 'Period name is required.';
    } else if (periodForm.name.length > 50) {
      errs.name = 'Name cannot exceed 50 characters.';
    }

    if (!periodForm.startDate) {
      errs.startDate = 'Start date is required.';
    }
    if (!periodForm.endDate) {
      errs.endDate = 'End date is required.';
    }

    if (periodForm.startDate && periodForm.endDate && periodForm.startDate > periodForm.endDate) {
      errs.endDate = 'End date cannot be prior to start date.';
    }

    setPeriodErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleCreatePeriod = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validatePeriodClientSide()) return;

    setPeriodSaving(true);
    try {
      await api.post('/api/accountings/periods', periodForm);
      toastEvents.success('Fiscal period created successfully');
      setIsPeriodModalOpen(false);
      setPeriodForm({ name: '', startDate: '', endDate: '', closed: false });
      setPeriodErrors({});
      fetchPeriods();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const errorMap: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((f: any) => {
          errorMap[f.field] = f.message;
        });
        setPeriodErrors(errorMap);
      }
    } finally {
      setPeriodSaving(false);
    }
  };

  const handleSortAccounts = (field: string) => {
    if (accountSortBy === field) {
      setAccountSortDir(accountSortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setAccountSortBy(field);
      setAccountSortDir('asc');
    }
  };

  const handleSortEntries = (field: string) => {
    if (entrySortBy === field) {
      setEntrySortDir(entrySortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setEntrySortBy(field);
      setEntrySortDir('asc');
    }
  };

  return (
    <div className="page-container">
      {/* HEADER SECTION */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <DollarSign size={24} />
          </div>
          <div>
            <h1 className="page-title">General Ledger & Accounting</h1>
            <p className="page-description">Double-entry ledger control, charts of accounts, and locked accounting cycles.</p>
          </div>
        </div>
      </div>

      {/* TABS SELECTOR */}
      <div className="glass-panel" style={{ display: 'flex', padding: '6px', gap: '8px', marginBottom: '1.5rem', maxWidth: '500px' }}>
        <button
          onClick={() => setActiveTab('accounts')}
          className="btn"
          style={{
            flex: 1,
            background: activeTab === 'accounts' ? 'var(--accent-gradient)' : 'transparent',
            color: activeTab === 'accounts' ? '#ffffff' : 'var(--text-secondary)'
          }}
        >
          <SlidersHorizontal size={16} /> Chart of Accounts
        </button>
        <button
          onClick={() => setActiveTab('entries')}
          className="btn"
          style={{
            flex: 1,
            background: activeTab === 'entries' ? 'var(--accent-gradient)' : 'transparent',
            color: activeTab === 'entries' ? '#ffffff' : 'var(--text-secondary)'
          }}
        >
          <Calendar size={16} /> Journal Entries
        </button>
        <button
          onClick={() => setActiveTab('periods')}
          className="btn"
          style={{
            flex: 1,
            background: activeTab === 'periods' ? 'var(--accent-gradient)' : 'transparent',
            color: activeTab === 'periods' ? '#ffffff' : 'var(--text-secondary)'
          }}
        >
          <Lock size={16} /> Fiscal Periods
        </button>
      </div>

      {/* ────────────────── CHART OF ACCOUNTS TAB ────────────────── */}
      {activeTab === 'accounts' && (
        <div>
          {/* Action Row */}
          <div className="filters-panel">
            <form onSubmit={handleAccountSearchSubmit} className="filters-group">
              <div className="search-input-wrapper">
                <Search size={18} className="search-icon" />
                <input
                  type="text"
                  placeholder="Search account code or name..."
                  value={accountSearch}
                  onChange={(e) => setAccountSearch(e.target.value)}
                  className="search-input"
                />
              </div>
              <select
                value={accountTypeFilter}
                onChange={(e) => setAccountTypeFilter(e.target.value)}
                className="form-control"
                style={{ width: 'auto' }}
              >
                <option value="">All Types</option>
                <option value="ASSET">Asset</option>
                <option value="LIABILITY">Liability</option>
                <option value="EQUITY">Equity</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </select>
              <button type="submit" className="btn btn-secondary"><ListFilter size={16} /> Filter</button>
            </form>

            <div className="header-actions">
              <button onClick={fetchAccounts} className="btn btn-secondary"><RefreshCw size={16} /></button>
              {hasWriteAccess && (
                <button onClick={handleOpenCreateAccount} className="btn btn-primary">
                  <Plus size={16} /> Create Account
                </button>
              )}
            </div>
          </div>

          {/* Table list */}
          <div className="table-wrapper">
            {accountsLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading Chart of Accounts...</div>
            ) : accounts.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No accounts configured.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr className="table-header-row">
                    <th onClick={() => handleSortAccounts('code')} style={{ padding: '16px', cursor: 'pointer' }}>Code <ArrowUpDown size={12} /></th>
                    <th onClick={() => handleSortAccounts('name')} style={{ padding: '16px', cursor: 'pointer' }}>Name <ArrowUpDown size={12} /></th>
                    <th onClick={() => handleSortAccounts('type')} style={{ padding: '16px', cursor: 'pointer' }}>Type <ArrowUpDown size={12} /></th>
                    <th style={{ padding: '16px' }}>Status</th>
                    <th style={{ padding: '16px', textAlign: 'right' }}>Debits</th>
                    <th style={{ padding: '16px', textAlign: 'right' }}>Credits</th>
                    <th style={{ padding: '16px', textAlign: 'right' }}>Net Balance</th>
                    {hasWriteAccess && <th style={{ padding: '16px', textAlign: 'center' }}>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {accounts.map((acc) => (
                    <tr key={acc.id} className="table-row">
                      <td style={{ fontWeight: 600 }}>{acc.code}</td>
                      <td>
                        <div>{acc.name}</div>
                        {acc.parentCode && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Parent: {acc.parentCode} - {acc.parentName}</span>}
                      </td>
                      <td>
                        <span style={{
                          padding: '3px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 600,
                          background: acc.type === 'ASSET' ? 'rgba(59, 130, 246, 0.15)' :
                                      acc.type === 'EXPENSE' ? 'rgba(239, 68, 68, 0.15)' :
                                      'rgba(16, 185, 129, 0.15)',
                          color: acc.type === 'ASSET' ? '#60a5fa' : acc.type === 'EXPENSE' ? '#f87171' : '#34d399'
                        }}>
                          {acc.type}
                        </span>
                      </td>
                      <td>
                        <span style={{ color: acc.active ? 'var(--success)' : 'var(--danger)', fontWeight: 500 }}>
                          {acc.active ? 'Active' : 'Deactivated'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right', fontFamily: 'monospace' }}>₹{acc.debitTotal.toFixed(2)}</td>
                      <td style={{ textAlign: 'right', fontFamily: 'monospace' }}>₹{acc.creditTotal.toFixed(2)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600, fontFamily: 'monospace', color: acc.netBalance >= 0 ? 'var(--text-primary)' : 'var(--danger)' }}>
                        ₹{acc.netBalance.toFixed(2)}
                      </td>
                      {hasWriteAccess && (
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button onClick={() => handleOpenEditAccount(acc)} className="btn btn-secondary" style={{ padding: '5px 8px' }} title="Edit"><Edit size={14} /></button>
                            <button onClick={() => handleDeactivateAccount(acc.id, acc.active)} className="btn btn-secondary" style={{ padding: '5px 8px', color: acc.active ? 'var(--warning)' : 'var(--success)' }} title={acc.active ? 'Deactivate' : 'Reactivate'}>
                              {acc.active ? <Lock size={14} /> : <Unlock size={14} />}
                            </button>
                            <button onClick={() => handleDeleteAccount(acc.id)} className="btn btn-secondary" style={{ padding: '5px 8px', color: 'var(--danger)' }} title="Delete"><Trash2 size={14} /></button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Pagination */}
          <div className="pagination-panel">
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total {accountTotalElements} accounts</span>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button disabled={accountPage === 0} onClick={() => setAccountPage(accountPage - 1)} className="btn btn-secondary" style={{ padding: '6px' }}><ChevronLeft size={16} /></button>
              <span style={{ fontSize: '0.9rem' }}>Page {accountPage + 1} of {accountTotalPages || 1}</span>
              <button disabled={accountPage >= accountTotalPages - 1} onClick={() => setAccountPage(accountPage + 1)} className="btn btn-secondary" style={{ padding: '6px' }}><ChevronRight size={16} /></button>
            </div>
          </div>
        </div>
      )}

      {/* ────────────────── JOURNAL ENTRIES TAB ────────────────── */}
      {activeTab === 'entries' && (
        <div>
          {/* Action/Filter Row */}
          <div className="filters-panel">
            <form onSubmit={handleEntrySearchSubmit} className="filters-group">
              <div className="search-input-wrapper">
                <Search size={18} className="search-icon" />
                <input
                  type="text"
                  placeholder="Search reference or memo..."
                  value={entrySearch}
                  onChange={(e) => setEntrySearch(e.target.value)}
                  className="search-input"
                />
              </div>
              <input
                type="date"
                value={entryStartDate}
                onChange={(e) => setEntryStartDate(e.target.value)}
                className="form-control"
                style={{ width: 'auto' }}
              />
              <input
                type="date"
                value={entryEndDate}
                onChange={(e) => setEntryEndDate(e.target.value)}
                className="form-control"
                style={{ width: 'auto' }}
              />
              <select
                value={entrySourceModule}
                onChange={(e) => setEntrySourceModule(e.target.value)}
                className="form-control"
                style={{ width: 'auto' }}
              >
                <option value="">All Sources</option>
                <option value="MANUAL">Manual</option>
                <option value="BILLING">Billing (Invoice)</option>
                <option value="PURCHASING">Purchasing</option>
                <option value="INVENTORY">Inventory Valuation</option>
              </select>
              <button type="submit" className="btn btn-secondary"><ListFilter size={16} /> Filter</button>
            </form>

            <div className="header-actions">
              <button onClick={fetchEntries} className="btn btn-secondary"><RefreshCw size={16} /></button>
              {hasWriteAccess && (
                <button onClick={() => { setEntryErrors({}); setIsPostModalOpen(true); }} className="btn btn-primary">
                  <Plus size={16} /> Post Manual Entry
                </button>
              )}
            </div>
          </div>

          {/* Table list */}
          <div className="table-wrapper">
            {entriesLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading Journal Entries...</div>
            ) : entries.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No entries found.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr className="table-header-row">
                    <th onClick={() => handleSortEntries('entryDate')} style={{ padding: '16px', cursor: 'pointer' }}>Date <ArrowUpDown size={12} /></th>
                    <th onClick={() => handleSortEntries('reference')} style={{ padding: '16px', cursor: 'pointer' }}>Reference <ArrowUpDown size={12} /></th>
                    <th style={{ padding: '16px' }}>Memo</th>
                    <th style={{ padding: '16px' }}>Source Module</th>
                    <th style={{ padding: '16px', textAlign: 'right' }}>Total Posted</th>
                    <th style={{ padding: '16px', textAlign: 'center' }}>Details</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => (
                    <tr key={entry.id} className="table-row">
                      <td style={{ fontWeight: 600 }}>{entry.entryDate}</td>
                      <td style={{ fontFamily: 'monospace' }}>{entry.reference}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{entry.memo || 'N/A'}</td>
                      <td>
                        <span className="badge badge-neutral">{entry.sourceModule}</span>
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 600, fontFamily: 'monospace' }}>
                        ₹{entry.totalAmount.toFixed(2)}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <button onClick={() => handleViewEntry(entry.id)} className="btn btn-secondary" style={{ padding: '5px 8px' }} title="View Details">
                          <Eye size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Pagination */}
          <div className="pagination-panel">
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total {entryTotalElements} entries</span>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button disabled={entryPage === 0} onClick={() => setEntryPage(entryPage - 1)} className="btn btn-secondary" style={{ padding: '6px' }}><ChevronLeft size={16} /></button>
              <span style={{ fontSize: '0.9rem' }}>Page {entryPage + 1} of {entryTotalPages || 1}</span>
              <button disabled={entryPage >= entryTotalPages - 1} onClick={() => setEntryPage(entryPage + 1)} className="btn btn-secondary" style={{ padding: '6px' }}><ChevronRight size={16} /></button>
            </div>
          </div>
        </div>
      )}

      {/* ────────────────── FISCAL PERIODS TAB ────────────────── */}
      {activeTab === 'periods' && (
        <div>
          {/* Action Row */}
          <div className="filters-panel">
            <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>Fiscal Periods</h2>
            <div className="header-actions">
              <button onClick={fetchPeriods} className="btn btn-secondary"><RefreshCw size={16} /></button>
              {isAdmin && (
                <button onClick={() => { setPeriodErrors({}); setIsPeriodModalOpen(true); }} className="btn btn-primary">
                  <Plus size={16} /> New Period
                </button>
              )}
            </div>
          </div>

          {/* Table list */}
          <div className="table-wrapper">
            {periodsLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading Fiscal Periods...</div>
            ) : periods.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No fiscal periods defined.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr className="table-header-row">
                    <th>Period Name</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Status</th>
                    {isAdmin && <th style={{ textAlign: 'center' }}>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {periods.map((period) => (
                    <tr key={period.id} className="table-row">
                      <td style={{ fontWeight: 600 }}>{period.name}</td>
                      <td>{period.startDate}</td>
                      <td>{period.endDate}</td>
                      <td>
                        <span className={`badge ${period.closed ? 'badge-danger' : 'badge-success'}`}>
                          {period.closed ? 'Closed / Locked' : 'Open'}
                        </span>
                      </td>
                      {isAdmin && (
                        <td style={{ textAlign: 'center' }}>
                          {period.closed ? (
                            <button onClick={() => handleReopenPeriod(period.id)} className="btn btn-secondary" style={{ padding: '5px 12px', borderColor: 'var(--success)', color: 'var(--success)' }}>
                              <Unlock size={14} /> Reopen
                            </button>
                          ) : (
                            <button onClick={() => handleClosePeriod(period.id)} className="btn btn-secondary" style={{ padding: '5px 12px', borderColor: 'var(--danger)', color: 'var(--danger)' }}>
                              <Lock size={14} /> Lock Period
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* ────────────────── MODAL: CREATE/EDIT ACCOUNT ────────────────── */}
      {isAccountModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{editingAccount ? 'Edit Account Details' : 'Create Ledger Account'}</h3>
              <button onClick={() => setIsAccountModalOpen(false)} className="close-button"><X size={20} /></button>
            </div>
            <form onSubmit={handleSaveAccount}>
              <div className="form-group">
                <label className="form-label">Account Code</label>
                <input
                    type="text"
                    required
                    value={accountForm.code}
                    className="form-control"
                  />
                  {accountErrors.code && (
                    <span className="error-text"><AlertCircle size={14} /> {accountErrors.code}</span>
                  )}
              </div>
              <div className="form-group">
                <label className="form-label">Account Name</label>
                <input
                    type="text"
                    required
                    value={accountForm.name}
                    className="form-control"
                  />
                  {accountErrors.name && (
                    <span className="error-text"><AlertCircle size={14} /> {accountErrors.name}</span>
                  )}
              </div>
              <div className="form-group">
                <label className="form-label">Account Type</label>
                <select
                    value={accountForm.type}
                    onChange={(e) => setAccountForm({ ...accountForm, type: e.target.value })}
                    style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: '#ffffff' }}
                  >
                    <option value="ASSET">Asset (Debit-normal)</option>
                    <option value="LIABILITY">Liability (Credit-normal)</option>
                    <option value="EQUITY">Equity (Credit-normal)</option>
                    <option value="INCOME">Income (Credit-normal)</option>
                    <option value="EXPENSE">Expense (Debit-normal)</option>
                  </select>
                  {accountErrors.type && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px', display: 'block' }}>{accountErrors.type}</span>
                  )}
                </div>
              <div className="form-group">
                <label className="form-label">Parent Account (Optional ID)</label>
                <input
                  type="text"
                  placeholder="Enter parent account ID..."
                  value={accountForm.parentId}
                  onChange={(e) => setAccountForm({ ...accountForm, parentId: e.target.value })}
                  className="form-control"
                />
                {accountErrors.parentId && (
                  <span className="error-text"><AlertCircle size={14} /> {accountErrors.parentId}</span>
                )}
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setIsAccountModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={accountSaving} className="btn btn-primary">
                  {accountSaving ? 'Saving Account...' : 'Save Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ────────────────── MODAL: VIEW JOURNAL DETAILS ────────────────── */}
      {isEntryDetailModalOpen && viewingEntry && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '750px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Journal Entry: {viewingEntry.reference}</h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Period: {viewingEntry.fiscalPeriodName} | Date: {viewingEntry.entryDate}</span>
              </div>
              <button onClick={() => setIsEntryDetailModalOpen(false)} className="close-button"><X size={20} /></button>
            </div>

            <div style={{ marginBottom: '1.5rem', padding: '12px', background: 'rgba(255,255,255,0.03)', borderRadius: '6px' }}>
              <span style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>Memo / Description:</span>
              <p style={{ fontSize: '0.95rem' }}>{viewingEntry.memo || 'No memo provided.'}</p>
            </div>

            <div style={{ overflowX: 'auto', marginBottom: '1.5rem' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Account Code / Name</th>
                    <th style={{ padding: '12px', textAlign: 'left' }}>Description</th>
                    <th style={{ padding: '12px', textAlign: 'right' }}>Debit</th>
                    <th style={{ padding: '12px', textAlign: 'right' }}>Credit</th>
                  </tr>
                </thead>
                <tbody>
                  {viewingEntry.lines.map((line) => (
                    <tr key={line.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                      <td style={{ padding: '12px' }}>
                        <div style={{ fontWeight: 600 }}>{line.accountCode}</div>
                        <div style={{ color: 'var(--text-secondary)' }}>{line.accountName}</div>
                      </td>
                      <td style={{ padding: '12px', color: 'var(--text-secondary)' }}>{line.description || 'N/A'}</td>
                      <td style={{ padding: '12px', textAlign: 'right', fontFamily: 'monospace' }}>
                        {line.debitAmount > 0 ? `₹${line.debitAmount.toFixed(2)}` : '-'}
                      </td>
                      <td style={{ padding: '12px', textAlign: 'right', fontFamily: 'monospace' }}>
                        {line.creditAmount > 0 ? `₹${line.creditAmount.toFixed(2)}` : '-'}
                      </td>
                    </tr>
                  ))}
                  <tr style={{ fontWeight: 600, borderTop: '2px solid var(--border-color)' }}>
                    <td colSpan={2} style={{ padding: '12px', textAlign: 'left' }}>Total Balance Checks</td>
                    <td style={{ padding: '12px', textAlign: 'right', fontFamily: 'monospace' }}>₹{viewingEntry.totalDebit.toFixed(2)}</td>
                    <td style={{ padding: '12px', textAlign: 'right', fontFamily: 'monospace' }}>₹{viewingEntry.totalCredit.toFixed(2)}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div className="modal-footer">
              <button onClick={() => setIsEntryDetailModalOpen(false)} className="btn btn-secondary">Close Details</button>
            </div>
          </div>
        </div>
      )}

      {/* ────────────────── MODAL: POST MANUAL ENTRY ────────────────── */}
      {isPostModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '850px' }}>
            <div className="modal-header">
              <h3 className="modal-title">Post Double-Entry Journal Document</h3>
              <button onClick={() => setIsPostModalOpen(false)} className="close-button"><X size={20} /></button>
            </div>

            <form onSubmit={handlePostJournalEntry} className="modal-body">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Posting Date</label>
                  <input
                    type="date"
                    required
                    value={postForm.entryDate}
                    onChange={(e) => setPostForm({ ...postForm, entryDate: e.target.value })}
                    className="form-control"
                  />
                  {entryErrors.entryDate && (
                    <span className="error-text"><AlertCircle size={14} /> {entryErrors.entryDate}</span>
                  )}
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Document Reference</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. JE-MAN-098"
                    value={postForm.reference}
                    onChange={(e) => setPostForm({ ...postForm, reference: e.target.value })}
                    className="form-control"
                  />
                  {entryErrors.reference && (
                    <span className="error-text"><AlertCircle size={14} /> {entryErrors.reference}</span>
                  )}
                </div>
                <div className="form-group" style={{ gridColumn: 'span 2', marginBottom: 0 }}>
                  <label className="form-label">Journal Document Memo</label>
                  <input
                    type="text"
                    placeholder="Enter context narrative description..."
                    value={postForm.memo}
                    onChange={(e) => setPostForm({ ...postForm, memo: e.target.value })}
                    className="form-control"
                  />
                  {entryErrors.memo && (
                    <span className="error-text"><AlertCircle size={14} /> {entryErrors.memo}</span>
                  )}
                </div>
              </div>

              {/* Journal Lines */}
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <h4 style={{ fontSize: '1rem', fontWeight: 600 }}>Ledger Lines (Debits and Credits)</h4>
                  <button type="button" onClick={handleAddPostLine} className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
                    <Plus size={14} /> Add Line
                  </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {postForm.lines.map((line, idx) => (
                    <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 2fr auto', gap: '10px', alignItems: 'center', background: 'var(--bg-tertiary)', padding: '10px', borderRadius: '6px' }}>
                      <select
                        required
                        value={line.accountId}
                        onChange={(e) => handleLineChange(idx, 'accountId', e.target.value)}
                        className="form-control"
                      >
                        <option value="">Select Account...</option>
                        {accounts.map(a => (
                          <option key={a.id} value={a.id}>{a.code} - {a.name} ({a.type})</option>
                        ))}
                      </select>
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Debit (₹)"
                        value={line.debitAmount || ''}
                        onChange={(e) => handleLineChange(idx, 'debitAmount', parseFloat(e.target.value) || 0)}
                        className="form-control"
                      />
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Credit (₹)"
                        value={line.creditAmount || ''}
                        onChange={(e) => handleLineChange(idx, 'creditAmount', parseFloat(e.target.value) || 0)}
                        className="form-control"
                      />
                      <input
                        type="text"
                        placeholder="Line description details..."
                        value={line.description}
                        onChange={(e) => handleLineChange(idx, 'description', e.target.value)}
                        className="form-control"
                      />
                      <button type="button" onClick={() => handleRemovePostLine(idx)} className="icon-button" style={{ color: 'var(--danger)' }}><Trash2 size={16} /></button>
                    </div>
                  ))}
                </div>
                {entryErrors.lines && (
                  <span style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '8px', display: 'block' }}>{entryErrors.lines}</span>
                )}
              </div>

              {/* Form totals check */}
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px', background: 'rgba(255,255,255,0.03)', borderRadius: '6px', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                <div>Total Debit: <strong style={{ fontFamily: 'monospace' }}>₹{postForm.lines.reduce((s,l)=>s+(l.debitAmount||0),0).toFixed(2)}</strong></div>
                <div>Total Credit: <strong style={{ fontFamily: 'monospace' }}>₹{postForm.lines.reduce((s,l)=>s+(l.creditAmount||0),0).toFixed(2)}</strong></div>
                <div>Balance status: <span style={{ color: postForm.lines.reduce((s,l)=>s+(l.debitAmount||0),0) === postForm.lines.reduce((s,l)=>s+(l.creditAmount||0),0) ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
                  {postForm.lines.reduce((s,l)=>s+(l.debitAmount||0),0) === postForm.lines.reduce((s,l)=>s+(l.creditAmount||0),0) ? 'Balanced' : 'Unbalanced'}
                </span></div>
              </div>
              {entryErrors.balance && (
                <span style={{ color: 'var(--danger)', fontSize: '0.85rem', fontWeight: 500, marginBottom: '1.5rem', display: 'block' }}>{entryErrors.balance}</span>
              )}

              <div className="modal-footer">
                <button type="button" onClick={() => setIsPostModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={entryPosting} className="btn btn-primary">
                  <CheckSquare size={16} /> {entryPosting ? 'Posting Journal...' : 'Post Journal Entry'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ────────────────── MODAL: CREATE FISCAL PERIOD ────────────────── */}
      {isPeriodModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Configure Fiscal Calendar Period</h3>
              <button onClick={() => setIsPeriodModalOpen(false)} className="close-button"><X size={20} /></button>
            </div>

            <form onSubmit={handleCreatePeriod} className="modal-body">
              <div className="form-group">
                <label className="form-label">Period Code Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. FY2026-Q3"
                  value={periodForm.name}
                  onChange={(e) => setPeriodForm({ ...periodForm, name: e.target.value })}
                  className="form-control"
                />
                {periodErrors.name && (
                  <span className="error-text"><AlertCircle size={14} /> {periodErrors.name}</span>
                )}
              </div>
              <div className="form-group">
                <label className="form-label">Start Date</label>
                <input
                  type="date"
                  required
                  value={periodForm.startDate}
                  onChange={(e) => setPeriodForm({ ...periodForm, startDate: e.target.value })}
                  className="form-control"
                />
                {periodErrors.startDate && (
                  <span className="error-text"><AlertCircle size={14} /> {periodErrors.startDate}</span>
                )}
              </div>
              <div className="form-group">
                <label className="form-label">End Date</label>
                <input
                  type="date"
                  required
                  value={periodForm.endDate}
                  onChange={(e) => setPeriodForm({ ...periodForm, endDate: e.target.value })}
                  className="form-control"
                />
                {periodErrors.endDate && (
                  <span className="error-text"><AlertCircle size={14} /> {periodErrors.endDate}</span>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" onClick={() => setIsPeriodModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={periodSaving} className="btn btn-primary">
                  {periodSaving ? 'Configuring...' : 'Create Period'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
