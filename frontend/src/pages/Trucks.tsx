import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import {
  Truck as TruckIcon, Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight,
  SlidersHorizontal, AlertCircle, X, Wrench, DollarSign,
  CheckCircle, User, Package, List, AlertTriangle, Grid
} from 'lucide-react';

import { TruckFormModal } from '../components/trucks/TruckFormModal';
import type { TruckFormData } from '../components/trucks/TruckFormModal';
import { DeliveryKanbanBoard } from '../components/trucks/DeliveryKanbanBoard';
import { DeliveryAssignmentModal } from '../components/trucks/DeliveryAssignmentModal';
import { MaintenanceLogModal } from '../components/trucks/MaintenanceLogModal';

interface TruckDto {
  id: number;
  registrationNumber: string;
  make: string;
  model: string;
  capacity: number | null;
  fuelType: string | null;
  driverEmployeeId: number | null;
  driverName: string | null;
  driverCode: string | null;
  lastServiceDate: string | null;
  maintenanceDue: boolean;
  isOptimistic?: boolean;
}

interface DeliveryAssignmentDto {
  id: number;
  truckId: number;
  truckRegistrationNumber: string;
  invoiceId: number | null;
  invoiceNumber: string | null;
  customerName: string | null;
  status: 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
}

interface MaintenanceLogDto {
  id: number;
  truckId: number;
  truckRegistrationNumber: string;
  date: string;
  type: string;
  cost: number;
  odometerReading: number | null;
  createdAt: string;
}

interface EmployeeOption {
  id: number;
  name: string;
  employeeCode: string;
}

interface InvoiceOption {
  id: number;
  invoiceNumber: string;
  customerName?: string;
}

interface AccountDto {
  id: number;
  code: string;
  name: string;
}

export const Trucks: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('TRUCK_WRITE');

  const [activeTab, setActiveTab] = useState<'fleet' | 'assignments' | 'maintenance'>('fleet');

  // Fleet Directory State
  const [trucks, setTrucks] = useState<TruckDto[]>([]);
  const [truckLoading, setTruckLoading] = useState(true);
  const [truckPage, setTruckPage] = useState(0);
  const [truckSize] = useState(10);
  const [truckTotalPages, setTruckTotalPages] = useState(0);
  const [truckTotalElements, setTruckTotalElements] = useState(0);

  // Filters for Fleet
  const [regSearch, setRegSearch] = useState('');
  const [makeSearch, setMakeSearch] = useState('');
  const [modelSearch, setModelSearch] = useState('');
  const [driverFilter, setDriverFilter] = useState('');

  // Assignments State
  const [assignments, setAssignments] = useState<DeliveryAssignmentDto[]>([]);
  const [assignLoading, setAssignLoading] = useState(true);
  const [assignPage, setAssignPage] = useState(0);
  const [assignSize] = useState(10);
  const [, setAssignTotalPages] = useState(0);
  const [assignTotalElements, setAssignTotalElements] = useState(0);
  const [assignTruckFilter, setAssignTruckFilter] = useState('');
  const [assignStatusFilter, setAssignStatusFilter] = useState('');
  const [assignViewMode, setAssignViewMode] = useState<'kanban' | 'table'>('kanban');

  // Maintenance State
  const [logs, setLogs] = useState<MaintenanceLogDto[]>([]);
  const [logsLoading, setLogsLoading] = useState(true);
  const [logsPage] = useState(0);
  const [logsSize] = useState(10);
  const [, setLogsTotalPages] = useState(0);
  const [logsTotalElements, setLogsTotalElements] = useState(0);
  const [logsTruckFilter] = useState('');

  // Helper Dropdown lists
  const [allDrivers, setAllDrivers] = useState<EmployeeOption[]>([]);
  const [allInvoices, setAllInvoices] = useState<InvoiceOption[]>([]);
  const [accounts, setAccounts] = useState<AccountDto[]>([]);

  // Modal States - Truck Create/Edit
  const [isTruckModalOpen, setIsTruckModalOpen] = useState(false);
  const [truckModalMode, setTruckModalMode] = useState<'create' | 'edit'>('create');
  const [selectedTruck, setSelectedTruck] = useState<TruckDto | null>(null);
  const [serverTruckErrors, setServerTruckErrors] = useState<Record<string, string>>({});
  const [serverGeneralError, setServerGeneralError] = useState<string | null>(null);
  const [submittingTruck, setSubmittingTruck] = useState(false);

  // Modal States - Assignment
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [submittingAssign, setSubmittingAssign] = useState(false);
  const [formAssignTruckId, setFormAssignTruckId] = useState('');
  const [formAssignInvoiceId, setFormAssignInvoiceId] = useState('');
  const [formAssignStatus, setFormAssignStatus] = useState<string>('ASSIGNED');
  const [serverAssignErrors, setServerAssignErrors] = useState<Record<string, string>>({});

  // Modal States - Maintenance Log
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [submittingLog, setSubmittingLog] = useState(false);
  const [formLogTruckId, setFormLogTruckId] = useState('');
  const [formLogDate, setFormLogDate] = useState(new Date().toISOString().split('T')[0]);
  const [formLogType, setFormLogType] = useState('Routine Service');
  const [formLogCost, setFormLogCost] = useState('');
  const [formLogOdometer, setFormLogOdometer] = useState('');
  const [serverLogErrors, setServerLogErrors] = useState<Record<string, string>>({});

  // Modal States - Expense Posting to GL
  const [isExpenseModalOpen, setIsExpenseModalOpen] = useState(false);
  const [submittingExpense, setSubmittingExpense] = useState(false);
  const [expTruckId, setExpTruckId] = useState('');
  const [expExpenseAccId, setExpExpenseAccId] = useState('');
  const [expPaymentAccId, setExpPaymentAccId] = useState('');
  const [expAmount, setExpAmount] = useState('');
  const [expDate, setExpDate] = useState(new Date().toISOString().split('T')[0]);
  const [expType, setExpType] = useState('FUEL');
  const [expMemo, setExpMemo] = useState('');
  const [serverExpenseErrors, setServerExpenseErrors] = useState<Record<string, string>>({});

  // ── FETCH LIFECYCLES ──────────────────────────────────────────────────

  const fetchTrucks = async () => {
    setTruckLoading(true);
    try {
      const params: any = {
        page: truckPage,
        size: truckSize,
        sort: ['registrationNumber,asc']
      };
      if (regSearch.trim()) params.registrationNumber = regSearch;
      if (makeSearch.trim()) params.make = makeSearch;
      if (modelSearch.trim()) params.model = modelSearch;
      if (driverFilter) params.driverId = driverFilter;

      const data = await fetchPaginated<TruckDto>('/api/trucks', params);
      setTrucks(data.content);
      setTruckTotalPages(data.totalPages);
      setTruckTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load fleet', err);
      toastEvents.error('Failed to load truck fleet');
    } finally {
      setTruckLoading(false);
    }
  };

  const fetchAssignments = async () => {
    setAssignLoading(true);
    try {
      const params: any = {
        page: assignPage,
        size: assignSize,
        sort: ['createdAt,desc']
      };
      if (assignTruckFilter) params.truckId = assignTruckFilter;
      if (assignStatusFilter) params.status = assignStatusFilter;

      const data = await fetchPaginated<DeliveryAssignmentDto>('/api/trucks/assignments', params);
      setAssignments(data.content);
      setAssignTotalPages(data.totalPages);
      setAssignTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load assignments', err);
      toastEvents.error('Failed to load delivery assignments');
    } finally {
      setAssignLoading(false);
    }
  };

  const fetchLogs = async () => {
    setLogsLoading(true);
    try {
      const params: any = {
        page: logsPage,
        size: logsSize,
        sort: ['date,desc']
      };
      if (logsTruckFilter) params.truckId = logsTruckFilter;

      const data = await fetchPaginated<MaintenanceLogDto>('/api/trucks/maintenance-logs', params);
      setLogs(data.content);
      setLogsTotalPages(data.totalPages);
      setLogsTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load maintenance logs', err);
      toastEvents.error('Failed to load maintenance logs');
    } finally {
      setLogsLoading(false);
    }
  };

  const loadDropdownHelpers = async () => {
    try {
      const empRes = await api.get<{ content: EmployeeOption[] }>('/api/employees?size=1000');
      setAllDrivers(empRes.data.content || []);

      const invRes = await api.get<{ content: InvoiceOption[] }>('/api/invoices?size=1000');
      setAllInvoices(invRes.data.content || []);

      const accRes = await api.get<{ content: AccountDto[] }>('/api/accountings/accounts?size=1000');
      setAccounts(accRes.data.content || []);
    } catch (err) {
      console.error('Failed to load dropdown helpers', err);
    }
  };

  useEffect(() => {
    loadDropdownHelpers();
  }, []);

  useEffect(() => {
    if (activeTab === 'fleet') {
      fetchTrucks();
    } else if (activeTab === 'assignments') {
      fetchAssignments();
    } else if (activeTab === 'maintenance') {
      fetchLogs();
    }
  }, [activeTab, truckPage, regSearch, makeSearch, modelSearch, driverFilter, assignPage, assignTruckFilter, assignStatusFilter, logsPage, logsTruckFilter]);

  const handleFleetSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setTruckPage(0);
    fetchTrucks();
  };

  // ── TRUCK FLEET ACTIONS ───────────────────────────────────────────────

  const openCreateTruckModal = () => {
    setTruckModalMode('create');
    setSelectedTruck(null);
    setServerTruckErrors({});
    setServerGeneralError(null);
    setIsTruckModalOpen(true);
  };

  const openEditTruckModal = (truck: TruckDto) => {
    setTruckModalMode('edit');
    setSelectedTruck(truck);
    setServerTruckErrors({});
    setServerGeneralError(null);
    setIsTruckModalOpen(true);
  };

  const handleSaveTruck = async (formData: TruckFormData) => {
    setSubmittingTruck(true);
    setServerTruckErrors({});
    setServerGeneralError(null);

    const assignedDriver = allDrivers.find(d => d.id.toString() === formData.driverEmployeeId);
    const tempId = selectedTruck ? selectedTruck.id : -Date.now();

    // Construct optimistic truck item for instant UI update
    const optimisticTruck: TruckDto = {
      id: tempId,
      registrationNumber: formData.registrationNumber.trim(),
      make: formData.make.trim(),
      model: formData.model.trim(),
      capacity: formData.capacity ? parseFloat(formData.capacity) : null,
      fuelType: formData.fuelType || 'DIESEL',
      driverEmployeeId: formData.driverEmployeeId ? parseInt(formData.driverEmployeeId) : null,
      driverName: assignedDriver ? assignedDriver.name : (selectedTruck?.driverName || null),
      driverCode: assignedDriver ? assignedDriver.employeeCode : (selectedTruck?.driverCode || null),
      lastServiceDate: formData.lastServiceDate || null,
      maintenanceDue: selectedTruck ? selectedTruck.maintenanceDue : false,
      isOptimistic: true
    };

    // Save previous state for rollback in case API request fails
    const previousTrucks = [...trucks];

    // Optimistic UI state mutation
    if (truckModalMode === 'create') {
      setTrucks([optimisticTruck, ...previousTrucks]);
      toastEvents.info(`Registering ${optimisticTruck.registrationNumber}...`);
    } else {
      setTrucks(previousTrucks.map(t => t.id === selectedTruck?.id ? optimisticTruck : t));
      toastEvents.info(`Updating ${optimisticTruck.registrationNumber}...`);
    }

    try {
      const payload = {
        registrationNumber: formData.registrationNumber.trim(),
        make: formData.make.trim(),
        model: formData.model.trim(),
        capacity: formData.capacity ? parseFloat(formData.capacity) : null,
        fuelType: formData.fuelType || null,
        driverEmployeeId: formData.driverEmployeeId ? parseInt(formData.driverEmployeeId) : null,
        lastServiceDate: formData.lastServiceDate || null
      };

      let responseData: TruckDto;
      if (truckModalMode === 'create') {
        const res = await api.post<TruckDto>('/api/trucks', payload);
        responseData = res.data;
        toastEvents.success(`Truck ${responseData.registrationNumber} registered successfully`);
      } else {
        const res = await api.put<TruckDto>(`/api/trucks/${selectedTruck?.id}`, payload);
        responseData = res.data;
        toastEvents.success(`Truck ${responseData.registrationNumber} updated successfully`);
      }

      // Replace optimistic item with real backend item
      setTrucks(current =>
        current.map(t => (t.id === tempId || t.id === selectedTruck?.id) ? { ...responseData, isOptimistic: false } : t)
      );

      setIsTruckModalOpen(false);
      fetchTrucks();
    } catch (err: any) {
      // Revert optimistic update on failure
      setTrucks(previousTrucks);

      // Parse standard API error shape (ErrorResponse with fieldErrors)
      if (err.response?.data?.fieldErrors && Array.isArray(err.response.data.fieldErrors)) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          if (fe.field && fe.message) {
            backendErrors[fe.field] = fe.message;
          }
        });
        setServerTruckErrors(backendErrors);
      }

      const generalMsg = err.response?.data?.message || 'Failed to save vehicle parameters.';
      setServerGeneralError(generalMsg);
      toastEvents.error(generalMsg);
    } finally {
      setSubmittingTruck(false);
    }
  };

  const handleDeleteTruck = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this truck? Vehicle with transaction history cannot be deleted.')) return;
    try {
      await api.delete(`/api/trucks/${id}`);
      toastEvents.success('Truck deleted successfully');
      fetchTrucks();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Cannot delete truck with active assignment or service history.');
    }
  };

  // ── ASSIGNMENT ACTIONS ────────────────────────────────────────────────

  const openCreateAssignModal = () => {
    setFormAssignTruckId(trucks.length > 0 ? trucks[0].id.toString() : '');
    setFormAssignInvoiceId('');
    setFormAssignStatus('ASSIGNED');
    setServerAssignErrors({});
    setIsAssignModalOpen(true);
  };

  const handleSaveAssignment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formAssignTruckId) {
      toastEvents.error('Please select a truck');
      return;
    }
    setSubmittingAssign(true);
    setServerAssignErrors({});
    try {
      await api.post('/api/trucks/assignments', {
        truckId: parseInt(formAssignTruckId),
        invoiceId: formAssignInvoiceId ? parseInt(formAssignInvoiceId) : null,
        status: formAssignStatus
      });
      toastEvents.success('Delivery assignment created');
      setIsAssignModalOpen(false);
      fetchAssignments();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors && Array.isArray(err.response.data.fieldErrors)) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          if (fe.field && fe.message) {
            backendErrors[fe.field] = fe.message;
          }
        });
        setServerAssignErrors(backendErrors);
      }
      toastEvents.error(err.response?.data?.message || 'Failed to create assignment');
    } finally {
      setSubmittingAssign(false);
    }
  };

  const handleUpdateAssignStatus = async (id: number, status: string) => {
    // Optimistic state update for instant drag-and-drop / click transition
    setAssignments(prev => prev.map(a => a.id === id ? { ...a, status: status as any } : a));
    try {
      await api.patch(`/api/trucks/assignments/${id}/status?status=${status}`);
      toastEvents.success(`Assignment status updated to ${status}`);
      fetchAssignments();
    } catch (err: any) {
      toastEvents.error('Failed to update status');
      fetchAssignments();
    }
  };

  const handleDeleteAssignment = async (id: number) => {
    if (!window.confirm('Delete this delivery assignment?')) return;
    try {
      await api.delete(`/api/trucks/assignments/${id}`);
      toastEvents.success('Assignment deleted');
      fetchAssignments();
    } catch (err: any) {
      toastEvents.error('Failed to delete assignment');
    }
  };

  // ── MAINTENANCE LOG ACTIONS ───────────────────────────────────────────

  const openCreateLogModal = (truckId?: number) => {
    setFormLogTruckId(truckId ? truckId.toString() : (trucks.length > 0 ? trucks[0].id.toString() : ''));
    setFormLogDate(new Date().toISOString().split('T')[0]);
    setFormLogType('Routine Service');
    setFormLogCost('');
    setFormLogOdometer('');
    setServerLogErrors({});
    setIsLogModalOpen(true);
  };

  const handleSaveLog = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formLogTruckId || !formLogCost) {
      toastEvents.error('Please select a truck and enter service cost');
      return;
    }
    setSubmittingLog(true);
    setServerLogErrors({});
    try {
      await api.post('/api/trucks/maintenance-logs', {
        truckId: parseInt(formLogTruckId),
        date: formLogDate,
        type: formLogType,
        cost: parseFloat(formLogCost),
        odometerReading: formLogOdometer ? parseFloat(formLogOdometer) : null
      });
      toastEvents.success('Maintenance service logged successfully');
      setIsLogModalOpen(false);
      fetchLogs();
      fetchTrucks();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors && Array.isArray(err.response.data.fieldErrors)) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          if (fe.field && fe.message) {
            backendErrors[fe.field] = fe.message;
          }
        });
        setServerLogErrors(backendErrors);
      }
      toastEvents.error(err.response?.data?.message || 'Failed to log maintenance');
    } finally {
      setSubmittingLog(false);
    }
  };

  // ── EXPENSE POSTING ACTIONS ───────────────────────────────────────────

  const openExpenseModal = (truckId?: number) => {
    setExpTruckId(truckId ? truckId.toString() : (trucks.length > 0 ? trucks[0].id.toString() : ''));
    setExpExpenseAccId('');
    setExpPaymentAccId('');
    setExpAmount('');
    setExpDate(new Date().toISOString().split('T')[0]);
    setExpType('FUEL');
    setExpMemo('');
    setServerExpenseErrors({});
    setIsExpenseModalOpen(true);
  };

  const handlePostExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!expTruckId || !expExpenseAccId || !expPaymentAccId || !expAmount) {
      toastEvents.error('Please fill in all required expense fields');
      return;
    }
    setSubmittingExpense(true);
    setServerExpenseErrors({});
    try {
      await api.post('/api/trucks/post-expense', {
        truckId: parseInt(expTruckId),
        expenseAccountId: parseInt(expExpenseAccId),
        paymentAccountId: parseInt(expPaymentAccId),
        amount: parseFloat(expAmount),
        date: expDate,
        expenseType: expType,
        memo: expMemo || null
      });
      toastEvents.success('Operating expense posted directly to General Ledger');
      setIsExpenseModalOpen(false);
    } catch (err: any) {
      if (err.response?.data?.fieldErrors && Array.isArray(err.response.data.fieldErrors)) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          if (fe.field && fe.message) {
            backendErrors[fe.field] = fe.message;
          }
        });
        setServerExpenseErrors(backendErrors);
      }
      toastEvents.error(err.response?.data?.message || 'Failed to post expense to ledger');
    } finally {
      setSubmittingExpense(false);
    }
  };

  return (
    <div style={{ padding: '1.5rem', width: '100%', minHeight: '85vh' }}>
      {/* HEADER */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TruckIcon size={24} style={{ color: 'var(--primary-color)' }} /> Fleet & Logistics Management
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.2rem' }}>
            Manage delivery trucks, dispatch assignments, service logs, and operating expense ledger integration.
          </p>
        </div>

        {/* Action buttons gated by write access */}
        {hasWriteAccess && (
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button onClick={() => openExpenseModal()} className="btn btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderColor: 'var(--primary-color)', color: 'var(--primary-color)' }}>
              <DollarSign size={16} /> Post Vehicle Expense
            </button>

            {activeTab === 'fleet' && (
              <button onClick={openCreateTruckModal} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Plus size={16} /> Add Truck
              </button>
            )}
            {activeTab === 'assignments' && (
              <button onClick={openCreateAssignModal} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Package size={16} /> New Assignment
              </button>
            )}
            {activeTab === 'maintenance' && (
              <button onClick={() => openCreateLogModal()} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Wrench size={16} /> Log Service
              </button>
            )}
          </div>
        )}
      </div>

      {/* TABS */}
      <div style={{ display: 'flex', borderBottom: '1px solid var(--border-color)', marginBottom: '1.5rem', gap: '1rem' }}>
        <button
          onClick={() => setActiveTab('fleet')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'fleet' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'fleet' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'fleet' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <TruckIcon size={16} /> Vehicle Fleet ({truckTotalElements})
        </button>
        <button
          onClick={() => setActiveTab('assignments')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'assignments' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'assignments' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'assignments' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <Package size={16} /> Delivery Assignments ({assignTotalElements})
        </button>
        <button
          onClick={() => setActiveTab('maintenance')}
          style={{
            padding: '0.75rem 1rem', background: 'none', border: 'none',
            borderBottom: activeTab === 'maintenance' ? '2px solid var(--primary-color)' : '2px solid transparent',
            color: activeTab === 'maintenance' ? 'var(--primary-color)' : 'var(--text-secondary)',
            fontWeight: activeTab === 'maintenance' ? 600 : 500, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem'
          }}
        >
          <Wrench size={16} /> Maintenance & Logs ({logsTotalElements})
        </button>
      </div>

      {/* TAB 1: FLEET DIRECTORY */}
      {activeTab === 'fleet' && (
        <>
          {/* SEARCH & FILTERS BAR */}
          <form onSubmit={handleFleetSearchSubmit} className="glass-panel" style={{ padding: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', flex: '1', minWidth: '200px', position: 'relative' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Registration No..."
                value={regSearch}
                onChange={(e) => setRegSearch(e.target.value)}
                style={{ width: '100%', padding: '8px 12px 8px 36px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
              />
            </div>

            <input
              type="text"
              placeholder="Make (e.g. Volvo)..."
              value={makeSearch}
              onChange={(e) => setMakeSearch(e.target.value)}
              style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
            />

            <input
              type="text"
              placeholder="Model..."
              value={modelSearch}
              onChange={(e) => setModelSearch(e.target.value)}
              style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
            />

            <select
              value={driverFilter}
              onChange={(e) => { setDriverFilter(e.target.value); setTruckPage(0); }}
              style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
            >
              <option value="">All Drivers</option>
              {allDrivers.map((d) => (
                <option key={d.id} value={d.id}>{d.name} ({d.employeeCode})</option>
              ))}
            </select>

            <button type="submit" className="btn btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '8px 16px' }}>
              <SlidersHorizontal size={14} /> Filter
            </button>
          </form>

          {/* FLEET TABLE */}
          <div className="glass-panel" style={{ overflowX: 'auto', borderRadius: '16px' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '850px' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(255, 255, 255, 0.02)' }}>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Reg Number</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Make & Model</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Capacity (Tons)</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Fuel Type</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Assigned Driver</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Last Service</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)', textAlign: 'center' }}>Service Status</th>
                  <th style={{ padding: '16px', fontWeight: 600, color: 'var(--text-secondary)', textAlign: 'center' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {truckLoading ? (
                  <tr>
                    <td colSpan={8} style={{ padding: '40px', textAlign: 'center' }}>
                      <div className="spinner" style={{ margin: '0 auto', width: '32px', height: '32px', border: '3px solid rgba(255,255,255,0.1)', borderTopColor: 'var(--primary-color)', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
                      <p style={{ color: 'var(--text-muted)', marginTop: '1rem', fontSize: '0.9rem' }}>Loading fleet registry...</p>
                    </td>
                  </tr>
                ) : trucks.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ padding: '40px', textAlign: 'center' }}>
                      <AlertCircle size={32} style={{ color: 'var(--text-muted)', marginBottom: '0.75rem' }} />
                      <p style={{ color: 'var(--text-secondary)', margin: 0, fontWeight: 500 }}>No trucks match filter criteria</p>
                    </td>
                  </tr>
                ) : (
                  trucks.map((truck) => (
                    <tr key={truck.id} className="table-row-hover" style={{ borderBottom: '1px solid var(--border-color)', transition: 'background 0.2s' }}>
                      <td style={{ padding: '16px', fontWeight: 600, color: 'var(--primary-color)' }}>
                        {truck.registrationNumber}
                        {truck.isOptimistic && (
                          <span style={{
                            marginLeft: '8px', padding: '2px 8px', borderRadius: '10px',
                            fontSize: '0.7rem', fontWeight: 600,
                            background: 'rgba(99, 102, 241, 0.2)', color: '#818cf8',
                            border: '1px solid rgba(99, 102, 241, 0.4)'
                          }}>
                            Syncing...
                          </span>
                        )}
                      </td>
                      <td style={{ padding: '16px', color: 'var(--text-primary)', fontWeight: 500 }}>{truck.make} {truck.model}</td>
                      <td style={{ padding: '16px', color: 'var(--text-primary)' }}>{truck.capacity ? `${truck.capacity} T` : 'N/A'}</td>
                      <td style={{ padding: '16px', color: 'var(--text-secondary)' }}>{truck.fuelType || 'DIESEL'}</td>
                      <td style={{ padding: '16px', color: 'var(--text-secondary)' }}>
                        {truck.driverName ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <User size={13} style={{ color: 'var(--primary-color)' }} />
                            <span>{truck.driverName} ({truck.driverCode})</span>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>Unassigned</span>
                        )}
                      </td>
                      <td style={{ padding: '16px', color: 'var(--text-muted)' }}>{truck.lastServiceDate || 'Never'}</td>
                      <td style={{ padding: '16px', textAlign: 'center' }}>
                        {truck.maintenanceDue ? (
                          <span style={{
                            padding: '4px 10px', borderRadius: '20px', fontSize: '0.75rem', fontWeight: 600,
                            background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', border: '1px solid rgba(239, 68, 68, 0.3)',
                            display: 'inline-flex', alignItems: 'center', gap: '4px'
                          }}>
                            <AlertTriangle size={12} /> Due for Service
                          </span>
                        ) : (
                          <span style={{
                            padding: '4px 10px', borderRadius: '20px', fontSize: '0.75rem', fontWeight: 600,
                            background: 'rgba(74, 222, 128, 0.15)', color: '#4ade80', border: '1px solid rgba(74, 222, 128, 0.3)',
                            display: 'inline-flex', alignItems: 'center', gap: '4px'
                          }}>
                            <CheckCircle size={12} /> Operational
                          </span>
                        )}
                      </td>
                      <td style={{ padding: '16px', textAlign: 'center' }}>
                        {hasWriteAccess ? (
                          <div style={{ display: 'flex', gap: '6px', justifyContent: 'center' }}>
                            <button onClick={() => openCreateLogModal(truck.id)} className="btn btn-secondary" style={{ padding: '6px 8px', borderRadius: '8px' }} title="Log Maintenance">
                              <Wrench size={13} />
                            </button>
                            <button onClick={() => openEditTruckModal(truck)} className="btn btn-secondary" style={{ padding: '6px 8px', borderRadius: '8px' }} title="Edit Vehicle">
                              <Edit size={13} />
                            </button>
                            <button onClick={() => handleDeleteTruck(truck.id)} className="btn btn-secondary" style={{ padding: '6px 8px', borderRadius: '8px', color: 'var(--danger)' }} title="Delete">
                              <Trash2 size={13} />
                            </button>
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Read Only</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            {/* PAGINATION */}
            {!truckLoading && truckTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Showing page {truckPage + 1} of {truckTotalPages} ({truckTotalElements} records)
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button onClick={() => setTruckPage(truckPage - 1)} disabled={truckPage === 0} className="btn btn-secondary" style={{ padding: '6px 12px', display: 'flex', alignItems: 'center' }}>
                    <ChevronLeft size={14} /> Prev
                  </button>
                  <button onClick={() => setTruckPage(truckPage + 1)} disabled={truckPage >= truckTotalPages - 1} className="btn btn-secondary" style={{ padding: '6px 12px', display: 'flex', alignItems: 'center' }}>
                    Next <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}

      {/* TAB 2: DELIVERY ASSIGNMENTS (KANBAN BOARD & TABLE) */}
      {activeTab === 'assignments' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* CONTROL BAR */}
          <div className="glass-panel" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', borderRadius: '16px' }}>
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
              <select
                value={assignTruckFilter}
                onChange={(e) => { setAssignTruckFilter(e.target.value); setAssignPage(0); }}
                style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
              >
                <option value="">All Fleet Trucks</option>
                {trucks.map((t) => (
                  <option key={t.id} value={t.id}>{t.registrationNumber} ({t.make})</option>
                ))}
              </select>

              <select
                value={assignStatusFilter}
                onChange={(e) => { setAssignStatusFilter(e.target.value); setAssignPage(0); }}
                style={{ padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
              >
                <option value="">All Status Columns</option>
                <option value="ASSIGNED">Assigned (Pending)</option>
                <option value="IN_TRANSIT">In Transit (Out for Delivery)</option>
                <option value="DELIVERED">Delivered</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            {/* VIEW TOGGLE BUTTONS */}
            <div style={{ display: 'flex', background: 'rgba(255,255,255,0.05)', padding: '4px', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
              <button
                onClick={() => setAssignViewMode('kanban')}
                style={{
                  padding: '6px 12px', borderRadius: '8px', border: 'none',
                  background: assignViewMode === 'kanban' ? 'var(--primary-color)' : 'transparent',
                  color: assignViewMode === 'kanban' ? '#fff' : 'var(--text-secondary)',
                  fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px'
                }}
              >
                <Grid size={14} /> Kanban Board
              </button>
              <button
                onClick={() => setAssignViewMode('table')}
                style={{
                  padding: '6px 12px', borderRadius: '8px', border: 'none',
                  background: assignViewMode === 'table' ? 'var(--primary-color)' : 'transparent',
                  color: assignViewMode === 'table' ? '#fff' : 'var(--text-secondary)',
                  fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px'
                }}
              >
                <List size={14} /> Table View
              </button>
            </div>
          </div>

          {/* KANBAN BOARD VIEW */}
          <DeliveryKanbanBoard
            assignments={assignments}
            loading={assignLoading}
            viewMode={assignViewMode}
            hasWriteAccess={hasWriteAccess}
            onUpdateStatus={handleUpdateAssignStatus}
            onDelete={handleDeleteAssignment}
          />
        </div>
      )}

      {/* TAB 3: MAINTENANCE LOGS */}
      {activeTab === 'maintenance' && (
        <div className="glass-panel" style={{ padding: '1.5rem', borderRadius: '16px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Date</th>
                <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Truck Reg</th>
                <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Service Type</th>
                <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Cost</th>
                <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Odometer</th>
              </tr>
            </thead>
            <tbody>
              {logsLoading ? (
                <tr><td colSpan={5} style={{ padding: '20px', textAlign: 'center' }}>Loading service logs...</td></tr>
              ) : logs.length === 0 ? (
                <tr><td colSpan={5} style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>No maintenance logs recorded</td></tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '12px', color: 'var(--text-primary)' }}>{log.date}</td>
                    <td style={{ padding: '12px', fontWeight: 600, color: 'var(--primary-color)' }}>{log.truckRegistrationNumber}</td>
                    <td style={{ padding: '12px' }}>{log.type}</td>
                    <td style={{ padding: '12px', fontWeight: 600 }}>₹{log.cost.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                    <td style={{ padding: '12px', color: 'var(--text-muted)' }}>{log.odometerReading ? `${log.odometerReading} km` : 'N/A'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* ── MODAL: CREATE / EDIT TRUCK ───────────────────────────────────── */}
      <TruckFormModal
        isOpen={isTruckModalOpen}
        mode={truckModalMode}
        truck={selectedTruck}
        allDrivers={allDrivers}
        isSubmitting={submittingTruck}
        serverErrors={serverTruckErrors}
        serverGeneralError={serverGeneralError}
        onClose={() => setIsTruckModalOpen(false)}
        onSubmit={handleSaveTruck}
      />

      {/* ── MODAL: DELIVERY ASSIGNMENT ───────────────────────────────────── */}
      <DeliveryAssignmentModal
        isOpen={isAssignModalOpen}
        trucks={trucks}
        invoices={allInvoices}
        isSubmitting={submittingAssign}
        serverErrors={serverAssignErrors}
        formAssignTruckId={formAssignTruckId}
        setFormAssignTruckId={setFormAssignTruckId}
        formAssignInvoiceId={formAssignInvoiceId}
        setFormAssignInvoiceId={setFormAssignInvoiceId}
        formAssignStatus={formAssignStatus}
        setFormAssignStatus={setFormAssignStatus}
        onClose={() => setIsAssignModalOpen(false)}
        onSubmit={handleSaveAssignment}
      />

      {/* ── MODAL: MAINTENANCE LOG ───────────────────────────────────────── */}
      <MaintenanceLogModal
        isOpen={isLogModalOpen}
        trucks={trucks}
        isSubmitting={submittingLog}
        serverErrors={serverLogErrors}
        formLogTruckId={formLogTruckId}
        setFormLogTruckId={setFormLogTruckId}
        formLogDate={formLogDate}
        setFormLogDate={setFormLogDate}
        formLogType={formLogType}
        setFormLogType={setFormLogType}
        formLogCost={formLogCost}
        setFormLogCost={setFormLogCost}
        formLogOdometer={formLogOdometer}
        setFormLogOdometer={setFormLogOdometer}
        onClose={() => setIsLogModalOpen(false)}
        onSubmit={handleSaveLog}
      />

      {/* ── MODAL: POST VEHICLE EXPENSE TO GL ─────────────────────────────── */}
      {isExpenseModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000, backdropFilter: 'blur(4px)'
        }}>
          <div className="glass-panel" style={{ width: '90%', maxWidth: '500px', background: 'var(--bg-card)', padding: '2rem', borderRadius: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <DollarSign size={18} style={{ color: 'var(--primary-color)' }} /> Post Vehicle Expense to GL
              </h2>
              <button onClick={() => setIsExpenseModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handlePostExpense}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Select Truck</label>
                  <select
                    value={expTruckId}
                    onChange={(e) => setExpTruckId(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="">Select Truck</option>
                    {trucks.map((t) => (
                      <option key={t.id} value={t.id}>{t.registrationNumber} ({t.make})</option>
                    ))}
                  </select>
                  {serverExpenseErrors.truckId && (
                    <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.truckId}</span>
                  )}
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Vehicle Expense Ledger Account (Debit)</label>
                  <select
                    value={expExpenseAccId}
                    onChange={(e) => setExpExpenseAccId(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="">Select Vehicle Operating Expense Account</option>
                    {accounts.filter(a => a.code.startsWith('5') || a.name.toLowerCase().includes('expense') || a.name.toLowerCase().includes('vehicle')).map((acc) => (
                      <option key={acc.id} value={acc.id}>{acc.code} - {acc.name}</option>
                    ))}
                  </select>
                  {serverExpenseErrors.expenseAccountId && (
                    <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.expenseAccountId}</span>
                  )}
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Payment Source Account (Credit)</label>
                  <select
                    value={expPaymentAccId}
                    onChange={(e) => setExpPaymentAccId(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="">Select Cash / Bank Account</option>
                    {accounts.filter(a => a.code.startsWith('101') || a.code.startsWith('102') || a.name.toLowerCase().includes('cash') || a.name.toLowerCase().includes('bank')).map((acc) => (
                      <option key={acc.id} value={acc.id}>{acc.code} - {acc.name}</option>
                    ))}
                  </select>
                  {serverExpenseErrors.paymentAccountId && (
                    <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.paymentAccountId}</span>
                  )}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Expense Amount (₹)</label>
                    <input
                      type="number"
                      step="0.01"
                      value={expAmount}
                      onChange={(e) => setExpAmount(e.target.value)}
                      required
                      placeholder="e.g. 150.00"
                      style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                    />
                    {serverExpenseErrors.amount && (
                      <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.amount}</span>
                    )}
                  </div>
                  <div>
                    <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Date</label>
                    <input
                      type="date"
                      value={expDate}
                      onChange={(e) => setExpDate(e.target.value)}
                      required
                      style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                    />
                    {serverExpenseErrors.date && (
                      <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.date}</span>
                    )}
                  </div>
                </div>

                <div>
                  <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Expense Type</label>
                  <select
                    value={expType}
                    onChange={(e) => setExpType(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                  >
                    <option value="FUEL">Fuel & Refuel</option>
                    <option value="MAINTENANCE">Routine Maintenance</option>
                    <option value="REPAIRS">Emergency Repairs</option>
                    <option value="TOLL_PARKING">Toll & Parking</option>
                  </select>
                  {serverExpenseErrors.expenseType && (
                    <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverExpenseErrors.expenseType}</span>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" onClick={() => setIsExpenseModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" disabled={submittingExpense} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <DollarSign size={16} /> {submittingExpense ? 'Posting Expense...' : 'Post to Ledger'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
