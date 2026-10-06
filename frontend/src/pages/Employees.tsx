import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import {
  Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight,
  RefreshCw, X, Eye, Lock, Unlock, Briefcase,
  DollarSign, Wallet, FileText, CheckCircle2
} from 'lucide-react';

interface EmployeeSummary {
  id: number;
  employeeCode: string;
  name: string;
  department: string;
  roleTitle: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'TERMINATED';
  joiningDate: string;
  locationName: string | null;
}

interface EmployeeDetail extends EmployeeSummary {
  contactDetails: string;
  salaryStructure: string | null;
  bankDetails: string | null;
  locationId: number | null;
  accountId: number | null;
  accountCode: string | null;
  accountName: string | null;
}



interface WorkEntryRecord {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  date: string;
  workedAmount: number;
  description: string | null;
  isSettled: boolean;
  createdAt: string;
}

interface SettlementRecord {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  settlementDate: string;
  amount: number;
  fundAccountId: number | null;
  fundAccountName: string | null;
  paymentMode: string;
  referenceNo: string | null;
  notes: string | null;
  createdAt: string;
}

interface EmployeeBalanceSummary {
  employeeId: number;
  employeeCode: string;
  name: string;
  department: string;
  roleTitle: string;
  totalWorkedAmount: number;
  totalSettledAmount: number;
  netOutstandingBalance: number;
}





export const Employees: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission, permissions } = useAuth();
  const hasWriteAccess = hasPermission('EMPLOYEE_WRITE');
  const isAdminOrHR = permissions.includes('ROLE_ADMINISTRATOR') || permissions.includes('ROLE_HR');

  const [activeTab, setActiveTab] = useState<'employees' | 'daily-work' | 'settlements'>('employees');

  // Employee Directory States
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [empLoading, setEmpLoading] = useState(true);
  const [empPage, setEmpPage] = useState(0);
  const [empSize] = useState(10);
  const [empTotalPages, setEmpTotalPages] = useState(0);
  const [empTotalElements, setEmpTotalElements] = useState(0);

  // Filters for Employees
  const [empSearch, setEmpSearch] = useState('');
  const [empStatusFilter, setEmpStatusFilter] = useState('');

  // Daily Work Entries States
  const [workEntries, setWorkEntries] = useState<WorkEntryRecord[]>([]);
  const [workLoading, setWorkLoading] = useState(false);
  const [workPage, setWorkPage] = useState(0);
  const [workSize] = useState(10);
  const [workTotalPages, setWorkTotalPages] = useState(0);
  const [workTotalElements, setWorkTotalElements] = useState(0);
  const [workEmpFilter, setWorkEmpFilter] = useState('');
  const [workStartFilter, setWorkStartFilter] = useState('');
  const [workEndFilter, setWorkEndFilter] = useState('');

  // Work Entry Modal State
  const [isWorkModalOpen, setIsWorkModalOpen] = useState(false);
  const [workModalMode] = useState<'create' | 'edit'>('create');
  const [selectedWorkId] = useState<number | null>(null);
  const [workEmpId, setWorkEmpId] = useState('');
  const [workDate, setWorkDate] = useState(new Date().toISOString().split('T')[0]);
  const [workAmount, setWorkAmount] = useState('');
  const [workDesc, setWorkDesc] = useState('');
  const [submittingWork, setSubmittingWork] = useState(false);

  // Settlements & Balances States
  const [balances, setBalances] = useState<EmployeeBalanceSummary[]>([]);
  const [settlements, setSettlements] = useState<SettlementRecord[]>([]);
  const [settleLoading, setSettleLoading] = useState(false);
  const [settlePage, setSettlePage] = useState(0);
  const [settleSize] = useState(10);
  const [settleTotalPages, setSettleTotalPages] = useState(0);
  const [settleTotalElements, setSettleTotalElements] = useState(0);
  const [settleEmpFilter, setSettleEmpFilter] = useState('');

  // Settlement Modal State
  const [isSettleModalOpen, setIsSettleModalOpen] = useState(false);
  const [settleEmpId, setSettleEmpId] = useState('');
  const [settleDate, setSettleDate] = useState(new Date().toISOString().split('T')[0]);
  const [settleAmount, setSettleAmount] = useState('');
  const [settlePaymentMode, setSettlePaymentMode] = useState('CASH');
  const [settleRefNo, setSettleRefNo] = useState('');
  const [settleNotes, setSettleNotes] = useState('');
  const [submittingSettle, setSubmittingSettle] = useState(false);

  // Dropdown lists
  const [allEmployees, setAllEmployees] = useState<EmployeeSummary[]>([]);

  // Modal States - Employee
  const [isEmpModalOpen, setIsEmpModalOpen] = useState(false);
  const [empModalMode, setEmpModalMode] = useState<'create' | 'edit' | 'view'>('create');
  const [selectedEmpId, setSelectedEmpId] = useState<number | null>(null);
  const [empErrors, setEmpErrors] = useState<Record<string, string>>({});
  const [submittingEmp, setSubmittingEmp] = useState(false);

  // Form Fields - Employee
  const [empCode, setEmpCode] = useState('');
  const [empName, setEmpName] = useState('');
  const [empContact, setEmpContact] = useState('');
  const [empRole, setEmpRole] = useState('');
  const [empJoining, setEmpJoining] = useState('');
  const [empStatusVal, setEmpStatusVal] = useState<'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'TERMINATED'>('ACTIVE');
  // ── FETCH LIFECYCLES ──────────────────────────────────────────────────

  const fetchEmployees = async () => {
    setEmpLoading(true);
    try {
      const params: any = {
        page: empPage,
        size: empSize,
        sort: ['employeeCode,asc']
      };
      if (empSearch.trim()) {
        params.name = empSearch;
        params.code = empSearch;
      }
      if (empStatusFilter) params.status = empStatusFilter;

      const data = await fetchPaginated<EmployeeSummary>('/api/employees', params);
      setEmployees(data.content);
      setEmpTotalPages(data.totalPages);
      setEmpTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load employees', err);
      toastEvents.error('Failed to load employees list');
    } finally {
      setEmpLoading(false);
    }
  };

  const fetchDailyWorkEntries = async () => {
    setWorkLoading(true);
    try {
      const params: any = {
        page: workPage,
        size: workSize,
        sort: ['date,desc']
      };
      if (workEmpFilter) params.employeeId = workEmpFilter;
      if (workStartFilter) params.startDate = workStartFilter;
      if (workEndFilter) params.endDate = workEndFilter;

      const data = await fetchPaginated<WorkEntryRecord>('/api/employees/work-entries', params);
      setWorkEntries(data.content);
      setWorkTotalPages(data.totalPages);
      setWorkTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load daily work entries', err);
      toastEvents.error('Failed to load daily work entries');
    } finally {
      setWorkLoading(false);
    }
  };

  const fetchBalancesAndSettlements = async () => {
    setSettleLoading(true);
    try {
      const [balRes, setRes] = await Promise.all([
        api.get<EmployeeBalanceSummary[]>('/api/employees/balances'),
        fetchPaginated<SettlementRecord>('/api/employees/settlements', {
          page: settlePage,
          size: settleSize,
          sort: ['settlementDate,desc'],
          employeeId: settleEmpFilter || undefined
        })
      ]);
      setBalances(balRes.data);
      setSettlements(setRes.content);
      setSettleTotalPages(setRes.totalPages);
      setSettleTotalElements(setRes.totalElements);
    } catch (err) {
      console.error('Failed to load employee balances / settlements', err);
      toastEvents.error('Failed to load settlements data');
    } finally {
      setSettleLoading(false);
    }
  };



  const loadDropdownData = async () => {
    try {
      const empRes = await api.get<{ content: EmployeeSummary[] }>('/api/employees?size=1000');
      setAllEmployees(empRes.data.content || []);
    } catch (err) {
      console.error('Failed to load dropdown helpers', err);
    }
  };

  useEffect(() => {
    loadDropdownData();
  }, []);

  useEffect(() => {
    if (activeTab === 'employees') {
      fetchEmployees();
    } else if (activeTab === 'daily-work') {
      fetchDailyWorkEntries();
    } else if (activeTab === 'settlements') {
      fetchBalancesAndSettlements();
    }
  }, [
    activeTab, empPage, empStatusFilter,
    workPage, workEmpFilter, workStartFilter, workEndFilter,
    settlePage, settleEmpFilter
  ]);

  const handleEmpSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setEmpPage(0);
    fetchEmployees();
  };

  // ── DAILY WORK HANDLERS ──────────────────────────────────────────────

  const openCreateWorkModal = (employeeId?: number) => {
    setWorkEmpId(employeeId ? employeeId.toString() : (allEmployees[0]?.id.toString() || ''));
    setWorkDate(new Date().toISOString().split('T')[0]);
    setWorkAmount('');
    setWorkDesc('');
    setIsWorkModalOpen(true);
  };

  const handleSaveWorkEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!workEmpId) {
      toastEvents.error('Please select an employee');
      return;
    }
    if (!workAmount || parseFloat(workAmount) <= 0) {
      toastEvents.error('Please enter a valid daily worked amount');
      return;
    }

    setSubmittingWork(true);
    try {
      const payload = {
        employeeId: parseInt(workEmpId),
        date: workDate,
        workedAmount: parseFloat(workAmount),
        description: workDesc || null
      };

      if (workModalMode === 'create') {
        await api.post('/api/employees/work-entries', payload);
        toastEvents.success('Daily worked amount logged successfully');
      } else if (selectedWorkId) {
        await api.put(`/api/employees/work-entries/${selectedWorkId}`, payload);
        toastEvents.success('Work entry updated successfully');
      }

      setIsWorkModalOpen(false);
      fetchDailyWorkEntries();
      if (activeTab === 'settlements') fetchBalancesAndSettlements();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to save work entry');
    } finally {
      setSubmittingWork(false);
    }
  };

  const handleDeleteWorkEntry = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this work entry?')) return;
    try {
      await api.delete(`/api/employees/work-entries/${id}`);
      toastEvents.success('Work entry deleted');
      fetchDailyWorkEntries();
    } catch (err: any) {
      toastEvents.error('Failed to delete work entry');
    }
  };

  // ── SETTLEMENT HANDLERS ──────────────────────────────────────────────

  const openCreateSettleModal = (employeeId?: number, defaultAmount?: number) => {
    const empIdStr = employeeId ? employeeId.toString() : (allEmployees[0]?.id.toString() || '');
    setSettleEmpId(empIdStr);
    setSettleDate(new Date().toISOString().split('T')[0]);
    setSettleAmount(defaultAmount && defaultAmount > 0 ? defaultAmount.toString() : '');
    setSettlePaymentMode('CASH');
    setSettleRefNo('');
    setSettleNotes('');
    setIsSettleModalOpen(true);
  };

  const handleSaveSettlement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settleEmpId) {
      toastEvents.error('Please select an employee');
      return;
    }
    if (!settleAmount || parseFloat(settleAmount) <= 0) {
      toastEvents.error('Please enter a valid settlement amount');
      return;
    }

    setSubmittingSettle(true);
    try {
      const payload = {
        employeeId: parseInt(settleEmpId),
        settlementDate: settleDate,
        amount: parseFloat(settleAmount),
        paymentMode: settlePaymentMode,
        referenceNo: settleRefNo || null,
        notes: settleNotes || null
      };

      await api.post('/api/employees/settlements', payload);
      toastEvents.success('Employee money settlement recorded successfully!');
      setIsSettleModalOpen(false);
      fetchBalancesAndSettlements();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to record settlement');
    } finally {
      setSubmittingSettle(false);
    }
  };

  // ── EMPLOYEE ACTIONS ──────────────────────────────────────────────────

  const openCreateEmpModal = () => {
    setEmpModalMode('create');
    setSelectedEmpId(null);
    setEmpErrors({});
    setEmpCode('');
    setEmpName('');
    setEmpContact('');
    setEmpRole('');
    setEmpJoining(new Date().toISOString().split('T')[0]);
    setEmpStatusVal('ACTIVE');
    setIsEmpModalOpen(true);
  };

  const openViewEmpModal = async (id: number) => {
    setEmpModalMode('view');
    setSelectedEmpId(id);
    try {
      const res = await api.get<EmployeeDetail>(`/api/employees/${id}`);
      const emp = res.data;
      setEmpCode(emp.employeeCode);
      setEmpName(emp.name);
      setEmpContact(emp.contactDetails || '');
      setEmpRole(emp.roleTitle);
      setEmpJoining(emp.joiningDate);
      setEmpStatusVal(emp.status);
      setIsEmpModalOpen(true);
    } catch (err) {
      toastEvents.error('Failed to load employee details');
    }
  };

  const openEditEmpModal = async (id: number) => {
    setEmpModalMode('edit');
    setSelectedEmpId(id);
    setEmpErrors({});
    try {
      const res = await api.get<EmployeeDetail>(`/api/employees/${id}`);
      const emp = res.data;
      setEmpCode(emp.employeeCode);
      setEmpName(emp.name);
      setEmpContact(emp.contactDetails || '');
      setEmpRole(emp.roleTitle);
      setEmpJoining(emp.joiningDate);
      setEmpStatusVal(emp.status);
      setIsEmpModalOpen(true);
    } catch (err) {
      toastEvents.error('Failed to load employee details');
    }
  };

  const handleSaveEmployee = async (e: React.FormEvent) => {
    e.preventDefault();
    setEmpErrors({});

    const clientErrors: Record<string, string> = {};
    if (!empCode.trim()) clientErrors.employeeCode = 'Employee Code is required';
    if (!empName.trim()) clientErrors.name = 'Employee Name is required';
    if (!empRole.trim()) clientErrors.roleTitle = 'Role Title is required';
    if (!empJoining) clientErrors.joiningDate = 'Joining Date is required';

    if (Object.keys(clientErrors).length > 0) {
      setEmpErrors(clientErrors);
      toastEvents.error('Please fix validation errors before submitting');
      return;
    }

    setSubmittingEmp(true);
    try {
      const payload: any = {
        employeeCode: empCode.trim(),
        name: empName.trim(),
        contactDetails: empContact.trim() || null,
        department: 'General',
        roleTitle: empRole.trim(),
        joiningDate: empJoining,
        status: empStatusVal,
        locationId: null
      };

      if (empModalMode === 'create') {
        await api.post('/api/employees', payload);
        toastEvents.success('Employee created successfully');
      } else {
        await api.put(`/api/employees/${selectedEmpId}`, payload);
        toastEvents.success('Employee updated successfully');
      }
      setIsEmpModalOpen(false);
      fetchEmployees();
      loadDropdownData();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          backendErrors[fe.field] = fe.message;
        });
        setEmpErrors(backendErrors);
      }
      toastEvents.error(err.response?.data?.message || 'Failed to save employee');
    } finally {
      setSubmittingEmp(false);
    }
  };

  const handleToggleEmpStatus = async (emp: EmployeeSummary) => {
    const isAct = emp.status === 'ACTIVE';
    const actionName = isAct ? 'deactivate' : 'activate';
    if (!window.confirm(`Are you sure you want to ${actionName} employee '${emp.name}'?`)) return;

    try {
      if (isAct) {
        await api.post(`/api/employees/${emp.id}/deactivate`);
        toastEvents.success(`Employee ${emp.name} deactivated`);
      } else {
        await api.post(`/api/employees/${emp.id}/activate`);
        toastEvents.success(`Employee ${emp.name} activated`);
      }
      fetchEmployees();
    } catch (err: any) {
      toastEvents.error(`Failed to ${actionName} employee`);
    }
  };

  const handleDeleteEmployee = async (id: number) => {
    if (!window.confirm('Are you sure you want to permanently delete this employee record?')) return;
    try {
      await api.delete(`/api/employees/${id}`);
      toastEvents.success('Employee deleted successfully');
      fetchEmployees();
      loadDropdownData();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to delete employee');
    }
  };





  const formatCurrency = (val: number) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val || 0);

  return (
    <div style={{ padding: '1.5rem', width: '100%', minHeight: '85vh' }}>
      {/* HEADER SECTION */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
            Employee Management
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.2rem' }}>
            {activeTab === 'employees' && `Track, manage and view profiles of your team members.`}
            {activeTab === 'daily-work' && `Log and manage daily worked amounts for individual employees.`}
            {activeTab === 'settlements' && `Track employee balances and process money settlements.`}
          </p>
        </div>

        {/* Global Header Actions */}
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            onClick={() => navigate('/reports')}
            className="btn btn-secondary"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderColor: 'var(--accent-primary)', color: 'var(--accent-primary)' }}
          >
            <FileText size={16} /> Running Sheet Report
          </button>

          {hasWriteAccess && (
            <>
              {activeTab === 'employees' && (
                <>
                  <button onClick={openCreateEmpModal} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Plus size={16} /> Add Employee
                  </button>
                </>
              )}

              {activeTab === 'daily-work' && (
                <button onClick={() => openCreateWorkModal()} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Plus size={16} /> Log Daily Work
                </button>
              )}

              {activeTab === 'settlements' && (
                <button onClick={() => openCreateSettleModal()} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Wallet size={16} /> Settle Money
                </button>
              )}


            </>
          )}
        </div>
      </div>

      {/* TABS SELECTOR */}
      <div style={{ display: 'flex', borderBottom: '1px solid var(--border-color)', marginBottom: '1.5rem', gap: '1rem', overflowX: 'auto' }}>
        <button
          onClick={() => setActiveTab('employees')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'employees' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'employees' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'employees' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <Briefcase size={16} /> Employee Directory
        </button>

        <button
          onClick={() => setActiveTab('daily-work')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'daily-work' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'daily-work' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'daily-work' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <DollarSign size={16} /> Daily Work Entries
        </button>

        <button
          onClick={() => setActiveTab('settlements')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'settlements' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'settlements' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'settlements' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <Wallet size={16} /> Money Settlements & Balances
        </button>


      </div>

      {/* ── TAB 1: EMPLOYEE DIRECTORY ────────────────────────────────────────── */}
      {activeTab === 'employees' && (
        <>
          {/* SEARCH & FILTERS BAR */}
          <form onSubmit={handleEmpSearchSubmit} className="glass-panel" style={{ padding: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', flex: '1', minWidth: '240px', position: 'relative' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Search code or name..."
                value={empSearch}
                onChange={(e) => setEmpSearch(e.target.value)}
                style={{
                  width: '100%', padding: '8px 12px 8px 36px', borderRadius: '10px',
                  border: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.05)',
                  color: 'var(--text-primary)', fontSize: '0.9rem'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
              <select
                value={empStatusFilter}
                onChange={(e) => { setEmpStatusFilter(e.target.value); setEmpPage(0); }}
                style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="TERMINATED">Terminated</option>
              </select>

              <button type="submit" className="btn btn-primary" style={{ padding: '8px 16px' }}>
                Filter
              </button>
            </div>
          </form>

          {/* TABLE CONTAINER */}
          <div className="glass-panel" style={{ overflow: 'hidden' }}>
            {empLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <RefreshCw size={24} className="spin" style={{ marginBottom: '0.5rem' }} />
                <p>Loading employees...</p>
              </div>
            ) : employees.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <Briefcase size={36} style={{ opacity: 0.4, marginBottom: '0.5rem' }} />
                <p>No employees found matching criteria.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)', color: 'var(--text-secondary)' }}>
                      <th style={{ padding: '1rem' }}>Code</th>
                      <th style={{ padding: '1rem' }}>Name</th>
                      <th style={{ padding: '1rem' }}>Role Title</th>
                      <th style={{ padding: '1rem' }}>Joining Date</th>
                      <th style={{ padding: '1rem' }}>Status</th>
                      <th style={{ padding: '1rem', textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {employees.map((emp) => (
                      <tr key={emp.id} style={{ borderBottom: '1px solid var(--border-color)', transition: 'background 0.2s' }}>
                        <td style={{ padding: '1rem', fontWeight: 600, color: 'var(--primary-color)' }}>{emp.employeeCode}</td>
                        <td style={{ padding: '1rem', fontWeight: 500, color: 'var(--text-primary)' }}>{emp.name}</td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{emp.roleTitle}</td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{emp.joiningDate}</td>
                        <td style={{ padding: '1rem' }}>
                          <span style={{
                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600,
                            background: emp.status === 'ACTIVE' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                            color: emp.status === 'ACTIVE' ? '#22c55e' : '#ef4444'
                          }}>
                            {emp.status}
                          </span>
                        </td>
                        <td style={{ padding: '1rem', textAlign: 'right' }}>
                          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                            <button
                              onClick={() => openCreateWorkModal(emp.id)}
                              title="Log Daily Work"
                              className="btn btn-icon"
                              style={{ color: '#3b82f6', background: 'rgba(59, 130, 246, 0.1)' }}
                            >
                              <DollarSign size={16} />
                            </button>
                            <button
                              onClick={() => openCreateSettleModal(emp.id)}
                              title="Settle Money"
                              className="btn btn-icon"
                              style={{ color: '#10b981', background: 'rgba(16, 185, 129, 0.1)' }}
                            >
                              <Wallet size={16} />
                            </button>
                            <button onClick={() => openViewEmpModal(emp.id)} title="View Employee" className="btn btn-icon">
                              <Eye size={16} />
                            </button>
                            {hasWriteAccess && (
                              <>
                                <button onClick={() => openEditEmpModal(emp.id)} title="Edit Employee" className="btn btn-icon">
                                  <Edit size={16} />
                                </button>
                                <button onClick={() => handleToggleEmpStatus(emp)} title={emp.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} className="btn btn-icon">
                                  {emp.status === 'ACTIVE' ? <Lock size={16} /> : <Unlock size={16} />}
                                </button>
                                {isAdminOrHR && (
                                  <button onClick={() => handleDeleteEmployee(emp.id)} title="Delete Employee" className="btn btn-icon" style={{ color: 'var(--danger)' }}>
                                    <Trash2 size={16} />
                                  </button>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* PAGINATION */}
            {empTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Page {empPage + 1} of {empTotalPages} ({empTotalElements} total)
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button disabled={empPage === 0} onClick={() => setEmpPage(empPage - 1)} className="btn btn-secondary">
                    <ChevronLeft size={16} /> Prev
                  </button>
                  <button disabled={empPage >= empTotalPages - 1} onClick={() => setEmpPage(empPage + 1)} className="btn btn-secondary">
                    Next <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}

      {/* ── TAB 2: DAILY WORK ENTRIES ────────────────────────────────────────── */}
      {activeTab === 'daily-work' && (
        <>
          {/* FILTER BAR */}
          <div className="glass-panel" style={{ padding: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
            <div style={{ minWidth: '220px' }}>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '2px' }}>Employee</label>
              <select
                value={workEmpFilter}
                onChange={(e) => { setWorkEmpFilter(e.target.value); setWorkPage(0); }}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              >
                <option value="">All Employees</option>
                {allEmployees.map(e => (
                  <option key={e.id} value={e.id}>{e.employeeCode} - {e.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '2px' }}>Start Date</label>
              <input
                type="date"
                value={workStartFilter}
                onChange={(e) => { setWorkStartFilter(e.target.value); setWorkPage(0); }}
                style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              />
            </div>

            <div>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '2px' }}>End Date</label>
              <input
                type="date"
                value={workEndFilter}
                onChange={(e) => { setWorkEndFilter(e.target.value); setWorkPage(0); }}
                style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              />
            </div>

            <button onClick={() => fetchDailyWorkEntries()} className="btn btn-secondary" style={{ marginTop: '18px' }}>
              <RefreshCw size={14} /> Refresh
            </button>
          </div>

          {/* TABLE CONTAINER */}
          <div className="glass-panel" style={{ overflow: 'hidden' }}>
            {workLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <RefreshCw size={24} className="spin" style={{ marginBottom: '0.5rem' }} />
                <p>Loading daily work entries...</p>
              </div>
            ) : workEntries.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <DollarSign size={36} style={{ opacity: 0.4, marginBottom: '0.5rem' }} />
                <p>No daily work entries found. Click "Log Daily Work" to add an entry.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)', color: 'var(--text-secondary)' }}>
                      <th style={{ padding: '1rem' }}>Date</th>
                      <th style={{ padding: '1rem' }}>Employee</th>
                      <th style={{ padding: '1rem' }}>Description / Task</th>
                      <th style={{ padding: '1rem', textAlign: 'right' }}>Worked Amount</th>
                      <th style={{ padding: '1rem', textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {workEntries.map((w) => (
                      <tr key={w.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '1rem', fontWeight: 500, color: 'var(--text-primary)' }}>{w.date}</td>
                        <td style={{ padding: '1rem' }}>
                          <span style={{ fontWeight: 600, color: 'var(--primary-color)' }}>{w.employeeCode}</span> - {w.employeeName}
                        </td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{w.description || 'Daily Worked Amount'}</td>
                        <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 600, color: '#10b981' }}>
                          {formatCurrency(w.workedAmount)}
                        </td>
                        <td style={{ padding: '1rem', textAlign: 'right' }}>
                          {hasWriteAccess && (
                            <button onClick={() => handleDeleteWorkEntry(w.id)} className="btn btn-icon" style={{ color: 'var(--danger)' }}>
                              <Trash2 size={16} />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {workTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Page {workPage + 1} of {workTotalPages} ({workTotalElements} total)
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button disabled={workPage === 0} onClick={() => setWorkPage(workPage - 1)} className="btn btn-secondary">
                    <ChevronLeft size={16} /> Prev
                  </button>
                  <button disabled={workPage >= workTotalPages - 1} onClick={() => setWorkPage(workPage + 1)} className="btn btn-secondary">
                    Next <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}

      {/* ── TAB 3: MONEY SETTLEMENTS & BALANCES ────────────────────────────── */}
      {activeTab === 'settlements' && (
        <>
          {/* EMPLOYEES OUTSTANDING BALANCES SUMMARY CARDS */}
          <div style={{ marginBottom: '2rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Wallet size={18} style={{ color: 'var(--accent-primary)' }} /> Employee Outstanding Balances
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 280px), 1fr))', gap: '1.25rem' }}>
              {balances.map(b => (
                <div key={b.employeeId} className="glass-panel" style={{ padding: '1.25rem', borderRadius: '16px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 700, color: 'var(--primary-color)', fontSize: '0.85rem' }}>{b.employeeCode}</span>
                    </div>
                    <h4 style={{ margin: '0.4rem 0 0.8rem 0', fontSize: '1.1rem', color: 'var(--text-primary)', fontWeight: 600 }}>{b.name}</h4>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                      <div>
                        <span>Total Worked:</span>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{formatCurrency(b.totalWorkedAmount)}</div>
                      </div>
                      <div>
                        <span>Total Settled:</span>
                        <div style={{ fontWeight: 600, color: '#3b82f6' }}>{formatCurrency(b.totalSettledAmount)}</div>
                      </div>
                    </div>
                  </div>

                  <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 600 }}>Net Unpaid Balance</span>
                      <div style={{ fontSize: '1.1rem', fontWeight: 700, color: b.netOutstandingBalance > 0 ? '#f59e0b' : '#10b981' }}>
                        {formatCurrency(b.netOutstandingBalance)}
                      </div>
                    </div>

                    {hasWriteAccess && b.netOutstandingBalance > 0 && (
                      <button
                        onClick={() => openCreateSettleModal(b.employeeId, b.netOutstandingBalance)}
                        className="btn btn-primary"
                        style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                      >
                        Settle Now
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* SETTLEMENT HISTORY TABLE */}
          <div className="glass-panel" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Settlement Payout History
              </h3>
              <select
                value={settleEmpFilter}
                onChange={(e) => { setSettleEmpFilter(e.target.value); setSettlePage(0); }}
                style={{ padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.85rem' }}
              >
                <option value="">All Employees</option>
                {allEmployees.map(e => (
                  <option key={e.id} value={e.id}>{e.employeeCode} - {e.name}</option>
                ))}
              </select>
            </div>

            {settleLoading ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <RefreshCw size={24} className="spin" style={{ marginBottom: '0.5rem' }} />
                <p>Loading settlement payouts...</p>
              </div>
            ) : settlements.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                <Wallet size={36} style={{ opacity: 0.4, marginBottom: '0.5rem' }} />
                <p>No settlements recorded yet.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)', color: 'var(--text-secondary)' }}>
                      <th style={{ padding: '1rem' }}>Date</th>
                      <th style={{ padding: '1rem' }}>Employee</th>
                      <th style={{ padding: '1rem' }}>Payment Mode / Fund</th>
                      <th style={{ padding: '1rem' }}>Reference / Notes</th>
                      <th style={{ padding: '1rem', textAlign: 'right' }}>Paid Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {settlements.map((s) => (
                      <tr key={s.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '1rem', fontWeight: 500, color: 'var(--text-primary)' }}>{s.settlementDate}</td>
                        <td style={{ padding: '1rem' }}>
                          <span style={{ fontWeight: 600, color: 'var(--primary-color)' }}>{s.employeeCode}</span> - {s.employeeName}
                        </td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                          <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{s.paymentMode}</span>
                          {s.fundAccountName && ` (${s.fundAccountName})`}
                        </td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                          {s.referenceNo ? `Ref: ${s.referenceNo}` : ''} {s.notes ? `- ${s.notes}` : ''}
                        </td>
                        <td style={{ padding: '1rem', textAlign: 'right', fontWeight: 700, color: '#3b82f6' }}>
                          {formatCurrency(s.amount)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {settleTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Page {settlePage + 1} of {settleTotalPages} ({settleTotalElements} total)
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button disabled={settlePage === 0} onClick={() => setSettlePage(settlePage - 1)} className="btn btn-secondary">
                    <ChevronLeft size={16} /> Prev
                  </button>
                  <button disabled={settlePage >= settleTotalPages - 1} onClick={() => setSettlePage(settlePage + 1)} className="btn btn-secondary">
                    Next <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}



      {/* ── MODAL: CREATE / EDIT EMPLOYEE ────────────────────────────────────── */}
      {isEmpModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000, backdropFilter: 'blur(4px)'
        }}>
          <div className="glass-panel" style={{
            width: '90%', maxWidth: '650px', maxHeight: '90vh', overflowY: 'auto',
            background: 'var(--bg-card)', padding: '2rem', borderRadius: '24px', boxShadow: '0 20px 40px rgba(0,0,0,0.4)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                {empModalMode === 'create' && 'Add New Employee'}
                {empModalMode === 'edit' && 'Edit Employee Details'}
                {empModalMode === 'view' && 'Employee Profile & Details'}
              </h2>
              <button onClick={() => setIsEmpModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveEmployee}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Employee Code *</label>
                  <input
                    type="text" value={empCode} onChange={(e) => setEmpCode(e.target.value)} disabled={empModalMode === 'view'}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: `1px solid ${empErrors.employeeCode ? 'var(--danger)' : 'var(--border-color)'}`, background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                  {empErrors.employeeCode && <span style={{ color: 'var(--danger)', fontSize: '0.75rem' }}>{empErrors.employeeCode}</span>}
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Full Name *</label>
                  <input
                    type="text" value={empName} onChange={(e) => setEmpName(e.target.value)} disabled={empModalMode === 'view'}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: `1px solid ${empErrors.name ? 'var(--danger)' : 'var(--border-color)'}`, background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                  {empErrors.name && <span style={{ color: 'var(--danger)', fontSize: '0.75rem' }}>{empErrors.name}</span>}
                </div>



                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Role Title *</label>
                  <input
                    type="text" value={empRole} onChange={(e) => setEmpRole(e.target.value)} disabled={empModalMode === 'view'}
                    placeholder="e.g. Senior Technician"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Joining Date *</label>
                  <input
                    type="date" value={empJoining} onChange={(e) => setEmpJoining(e.target.value)} disabled={empModalMode === 'view'}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Status *</label>
                  <select
                    value={empStatusVal} onChange={(e) => setEmpStatusVal(e.target.value as any)} disabled={empModalMode === 'view'}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="SUSPENDED">Suspended</option>
                    <option value="TERMINATED">Terminated</option>
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Contact Phone / Email</label>
                  <input
                    type="text" value={empContact} onChange={(e) => setEmpContact(e.target.value)} disabled={empModalMode === 'view'}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '2rem' }}>
                <button type="button" onClick={() => setIsEmpModalOpen(false)} className="btn btn-secondary">Cancel</button>
                {empModalMode !== 'view' && (
                  <button type="submit" disabled={submittingEmp} className="btn btn-primary">
                    {submittingEmp ? 'Saving...' : 'Save Employee'}
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: LOG DAILY WORK ────────────────────────────────────────── */}
      {isWorkModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000, backdropFilter: 'blur(4px)'
        }}>
          <div className="glass-panel" style={{
            width: '90%', maxWidth: '500px', background: 'var(--bg-card)', padding: '2rem', borderRadius: '24px', boxShadow: '0 20px 40px rgba(0,0,0,0.4)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <DollarSign size={20} style={{ color: '#10b981' }} /> Log Daily Worked Amount
              </h2>
              <button onClick={() => setIsWorkModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveWorkEntry}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Employee *</label>
                  <select
                    value={workEmpId} onChange={(e) => setWorkEmpId(e.target.value)} required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="">-- Select Employee --</option>
                    {allEmployees.map(e => (
                      <option key={e.id} value={e.id}>{e.employeeCode} - {e.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Date *</label>
                  <input
                    type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Worked Amount (₹) *</label>
                  <input
                    type="number" step="0.01" min="0.01" value={workAmount} onChange={(e) => setWorkAmount(e.target.value)} required placeholder="e.g. 1500"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '1rem', fontWeight: 600 }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Work Notes / Task Description</label>
                  <input
                    type="text" value={workDesc} onChange={(e) => setWorkDesc(e.target.value)} placeholder="e.g. Shift work / overtime"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" onClick={() => setIsWorkModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={submittingWork} className="btn btn-primary">
                  {submittingWork ? 'Saving...' : 'Save Work Entry'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: SETTLE MONEY ────────────────────────────────────────── */}
      {isSettleModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000, backdropFilter: 'blur(4px)'
        }}>
          <div className="glass-panel" style={{
            width: '90%', maxWidth: '520px', background: 'var(--bg-card)', padding: '2rem', borderRadius: '24px', boxShadow: '0 20px 40px rgba(0,0,0,0.4)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Wallet size={20} style={{ color: '#3b82f6' }} /> Settle Money for Employee
              </h2>
              <button onClick={() => setIsSettleModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveSettlement}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Employee *</label>
                  <select
                    value={settleEmpId} onChange={(e) => setSettleEmpId(e.target.value)} required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="">-- Select Employee --</option>
                    {allEmployees.map(e => (
                      <option key={e.id} value={e.id}>{e.employeeCode} - {e.name}</option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '1rem' }}>
                  <div>
                    <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Settlement Date *</label>
                    <input
                      type="date" value={settleDate} onChange={(e) => setSettleDate(e.target.value)} required
                      style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                    />
                  </div>

                  <div>
                    <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Settlement Amount (₹) *</label>
                    <input
                      type="number" step="0.01" min="0.01" value={settleAmount} onChange={(e) => setSettleAmount(e.target.value)} required placeholder="e.g. 5000"
                      style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '1rem', fontWeight: 600 }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Payment Mode</label>
                  <select
                    value={settlePaymentMode} onChange={(e) => setSettlePaymentMode(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="CASH">Cash</option>
                    <option value="BANK_TRANSFER">Bank Transfer</option>
                    <option value="UPI">UPI / GPay</option>
                    <option value="CHEQUE">Cheque</option>
                  </select>
                </div>

                <div style={{ background: 'rgba(59, 130, 246, 0.08)', border: '1px solid rgba(59, 130, 246, 0.2)', padding: '0.75rem 1rem', borderRadius: '10px', fontSize: '0.8rem', color: '#93c5fd' }}>
                  ℹ️ <strong>Record-keeping only:</strong> Settlement records track individual employee running balance sheets and do not alter accounting or fund account ledgers.
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Reference Number</label>
                  <input
                    type="text" value={settleRefNo} onChange={(e) => setSettleRefNo(e.target.value)} placeholder="Txn ID / Cheque # / Ref"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Notes / Remarks</label>
                  <input
                    type="text" value={settleNotes} onChange={(e) => setSettleNotes(e.target.value)} placeholder="e.g. Weekly settlement payout"
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" onClick={() => setIsSettleModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={submittingSettle} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <CheckCircle2 size={16} /> {submittingSettle ? 'Recording...' : 'Confirm Settlement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}




    </div>
  );
};
