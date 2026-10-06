import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { 
  Plus, Search, Edit, Trash2, UserX, ChevronLeft, ChevronRight, 
  ArrowUpDown, SlidersHorizontal, AlertCircle, RefreshCw, X, FileText, Users
} from 'lucide-react';

interface CustomerSummary {
  id: number;
  customerCode: string;
  name: string;
  businessName: string;
  phone: string;
  email: string;
  creditLimit: number | null;
  creditHold: boolean;
  status: 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED';
}

interface CustomerDetail extends CustomerSummary {
  address: string;
  taxId: string;
  openingBalance: number;
  priceTierId: number | null;
}

export const Customers: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('CUSTOMER_WRITE');
  const navigate = useNavigate();

  // List & Filter States
  const [customers, setCustomers] = useState<CustomerSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortField, setSortField] = useState('customerCode');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  // Modal States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Form Fields
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [taxId, setTaxId] = useState('');
  const [creditLimit, setCreditLimit] = useState('');
  const [creditHold, setCreditHold] = useState(false);
  const [priceTierId, setPriceTierId] = useState('');
  const [openingBalance, setOpeningBalance] = useState('0.00');
  const [statusVal, setStatusVal] = useState<'ACTIVE' | 'INACTIVE' | 'BLACKLISTED'>('ACTIVE');

  const fetchCustomers = async () => {
    setLoading(true);
    try {
      const sortParam = [`${sortField},${sortOrder}`];
      const params: any = {
        page,
        size,
        sort: sortParam
      };

      if (search.trim()) {
        // Query both name and code by passing them in params
        params.name = search;
        params.code = search;
      }
      if (statusFilter) {
        params.status = statusFilter;
      }

      const data = await fetchPaginated<CustomerSummary>('/api/customers', params);
      setCustomers(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load customers', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomers();
  }, [page, size, statusFilter, sortField, sortOrder]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchCustomers();
  };

  const handleSort = (field: string) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
    setPage(0);
  };

  // Open Create Modal
  const openCreateModal = () => {
    setModalMode('create');
    setSelectedCustomerId(null);
    setErrors({});
    setCode('');
    setName('');
    setBusinessName('');
    setPhone('');
    setEmail('');
    setAddress('');
    setTaxId('');
    setCreditLimit('');
    setCreditHold(false);
    setPriceTierId('');
    setOpeningBalance('0.00');
    setStatusVal('ACTIVE');
    setIsModalOpen(true);
  };

  // Open Edit Modal
  const openEditModal = async (id: number) => {
    setModalMode('edit');
    setSelectedCustomerId(id);
    setErrors({});
    setLoading(true);
    try {
      const response = await api.get<CustomerDetail>(`/api/customers/${id}`);
      const data = response.data;
      setCode(data.customerCode);
      setName(data.name);
      setBusinessName(data.businessName || '');
      setPhone(data.phone || '');
      setEmail(data.email || '');
      setAddress(data.address || '');
      setTaxId(data.taxId || '');
      setCreditLimit(data.creditLimit ? data.creditLimit.toString() : '');
      setCreditHold(data.creditHold);
      setPriceTierId(data.priceTierId ? data.priceTierId.toString() : '');
      setOpeningBalance(data.openingBalance.toString());
      setStatusVal(data.status);
      setIsModalOpen(true);
    } catch (err) {
      console.error('Failed to load customer details', err);
    } finally {
      setLoading(false);
    }
  };

  // Save Modal
  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    // Client-side validations
    const clientErrors: Record<string, string> = {};
    if (!name.trim()) clientErrors.name = 'Name is required';
    if (modalMode === 'create' && !code.trim()) clientErrors.customerCode = 'Customer code is required';
    if (email.trim() && !/\S+@\S+\.\S+/.test(email)) clientErrors.email = 'Invalid email format';
    if (creditLimit && parseFloat(creditLimit) < 0) clientErrors.creditLimit = 'Credit limit must be positive or zero';
    if (openingBalance && parseFloat(openingBalance) < 0) clientErrors.openingBalance = 'Opening balance must be positive or zero';

    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      toastEvents.error('Validation failed. Please correct form fields.');
      return;
    }

    setSubmitting(true);
    try {
      const payload: any = {
        name,
        businessName,
        phone,
        email,
        address,
        taxId,
        creditLimit: creditLimit ? parseFloat(creditLimit) : null,
        creditHold,
        priceTierId: priceTierId ? parseInt(priceTierId) : null,
        status: statusVal
      };

      if (modalMode === 'create') {
        payload.customerCode = code;
        payload.openingBalance = parseFloat(openingBalance) || 0;

        // Optimistic UI update
        const optimisticCustomer: CustomerSummary = {
          id: Math.random(),
          customerCode: code,
          name,
          businessName,
          phone,
          email,
          creditLimit: creditLimit ? parseFloat(creditLimit) : null,
          creditHold,
          status: statusVal
        };
        setCustomers((prev) => [optimisticCustomer, ...prev]);

        await api.post('/api/customers', payload);
        toastEvents.success('Customer created successfully');
      } else {
        // Optimistic UI update
        setCustomers((prev) =>
          prev.map((c) =>
            c.id === selectedCustomerId
              ? {
                  ...c,
                  name,
                  businessName,
                  phone,
                  email,
                  creditLimit: creditLimit ? parseFloat(creditLimit) : null,
                  creditHold,
                  status: statusVal
                }
              : c
          )
        );

        await api.put(`/api/customers/${selectedCustomerId}`, payload);
        toastEvents.success('Customer updated successfully');
      }
      setIsModalOpen(false);
      fetchCustomers();
    } catch (err: any) {
      console.error('Save failed', err);
      if (err.response?.status === 400 && err.response?.data?.fieldErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          // Map to standard fields. Note: backend might return customerCode or status.
          backendErrors[fe.field] = fe.message;
        });
        setErrors(backendErrors);
        toastEvents.error('Validation failed. Please correct form fields.');
      } else if (err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('An unexpected error occurred.');
      }
      // Revert optimistic updates on failure
      fetchCustomers();
    } finally {
      setSubmitting(false);
    }
  };

  // Delete Customer
  const handleDelete = async (id: number, code: string) => {
    if (!window.confirm(`Are you sure you want to delete customer ${code}?`)) return;
    try {
      await api.delete(`/api/customers/${id}`);
      toastEvents.success('Customer deleted successfully');
      fetchCustomers();
    } catch (err) {
      console.error('Delete failed', err);
    }
  };

  // Deactivate Customer Toggle
  const handleDeactivate = async (id: number) => {
    try {
      await api.put(`/api/customers/${id}/deactivate`);
      toastEvents.success('Customer deactivated successfully');
      fetchCustomers();
    } catch (err) {
      console.error('Deactivation failed', err);
    }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Users size={22} />
          </div>
          <div>
            <h1 className="page-title">Customer Directory</h1>
            <p className="page-description">
              Manage client profiles, lines of credit, and account billing statuses
            </p>
          </div>
        </div>
        <div className="header-actions">
          {hasWriteAccess && (
            <button onClick={openCreateModal} className="btn btn-primary">
              <Plus size={18} />
              <span>Add Customer</span>
            </button>
          )}
        </div>
      </div>

      {/* Filters Panel */}
      <div className="filters-panel">
        <form onSubmit={handleSearchSubmit} className="filters-group">
          <div className="search-input-wrapper">
            <Search size={16} className="search-icon" />
            <input
              type="text"
              className="search-input"
              placeholder="Search by code or name..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-secondary">Search</button>
        </form>

        <div className="filters-group" style={{ flex: 'none' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <SlidersHorizontal size={16} style={{ color: 'var(--text-muted)' }} />
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Status:</span>
          </div>
          <select
            className="form-select"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            style={{ width: 'auto' }}
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="BLACKLISTED">Blacklisted</option>
          </select>
        </div>
      </div>

      {/* Table Panel */}
      <div className="table-wrapper">
        {loading && customers.length === 0 ? (
          <div style={{ padding: '4rem', textAlign: 'center' }}>
            <RefreshCw size={24} style={{ animation: 'spin 1s linear infinite', color: 'var(--accent-primary)', marginBottom: '1rem' }} />
            <p style={{ color: 'var(--text-secondary)' }}>Retrieving records...</p>
          </div>
        ) : customers.length === 0 ? (
          <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <AlertCircle size={32} style={{ marginBottom: '1rem' }} />
            <p>No customers found matching the search criteria.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr className="table-header-row">
                  <th onClick={() => handleSort('customerCode')}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span>Code</span>
                      <ArrowUpDown size={12} />
                    </div>
                  </th>
                  <th onClick={() => handleSort('name')}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span>Name</span>
                      <ArrowUpDown size={12} />
                    </div>
                  </th>
                  <th>Phone / Email</th>
                  <th>Credit Limit</th>
                  <th>Credit Hold</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((c) => (
                  <tr key={c.id} className="table-row">
                    <td style={{ fontWeight: 600 }}>{c.customerCode}</td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <span style={{ fontWeight: 500 }}>{c.name}</span>
                        {c.businessName && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>{c.businessName}</span>}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                        {c.phone && <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{c.phone}</span>}
                        {c.email && <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{c.email}</span>}
                      </div>
                    </td>
                    <td>
                      {c.creditLimit ? `₹${c.creditLimit.toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : <span style={{ color: 'var(--text-muted)' }}>Unlimited</span>}
                    </td>
                    <td>
                      <span className={c.creditHold ? 'badge badge-danger' : 'badge badge-success'}>
                        {c.creditHold ? 'ON HOLD' : 'OK'}
                      </span>
                    </td>
                    <td>
                      <span className={c.status === 'ACTIVE' ? 'badge badge-info' : c.status === 'BLACKLISTED' ? 'badge badge-warning' : 'badge badge-neutral'}>
                        {c.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                        <button onClick={() => navigate(`/customers/${c.id}/statement`)} className="btn btn-outline btn-sm" title="Customer Statement">
                          <FileText size={16} />
                        </button>
                        
                        {hasWriteAccess && (
                          <>
                            <button onClick={() => openEditModal(c.id)} className="btn btn-outline btn-sm" title="Edit Customer">
                              <Edit size={16} />
                            </button>
                            {c.status === 'ACTIVE' && (
                              <button onClick={() => handleDeactivate(c.id)} className="btn btn-outline btn-sm" title="Deactivate Customer">
                                <UserX size={16} />
                              </button>
                            )}
                            <button onClick={() => handleDelete(c.id, c.customerCode)} className="btn btn-danger btn-sm" title="Delete Customer">
                              <Trash2 size={16} />
                            </button>
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

        {/* Pagination Panel */}
        {totalPages > 0 && (
          <div className="pagination-panel">
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              Showing {page * size + 1} - {Math.min((page + 1) * size, totalElements)} of {totalElements} customers
            </span>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="btn btn-secondary"
                style={{ opacity: page === 0 ? 0.4 : 1 }}
              >
                <ChevronLeft size={16} />
                <span>Prev</span>
              </button>
              <span style={{ fontSize: '0.85rem', margin: '0 0.5rem' }}>
                Page {page + 1} of {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="btn btn-secondary"
                style={{ opacity: page >= totalPages - 1 ? 0.4 : 1 }}
              >
                <span>Next</span>
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal - Create/Edit Overlay */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            {/* Modal Header */}
            <div className="modal-header">
              <h2 className="modal-title">
                {modalMode === 'create' ? 'Create New Customer' : 'Edit Customer Profile'}
              </h2>
              <button onClick={() => setIsModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
                
                {/* Code */}
                <div className="form-group">
                  <label className="form-label">
                    Customer Code <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={modalMode === 'edit' || submitting}
                    placeholder="e.g. CUST-001"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    style={{ border: errors.customerCode ? '1px solid var(--danger)' : undefined, opacity: modalMode === 'edit' ? 0.6 : 1 }}
                  />
                  {errors.customerCode && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.customerCode}
                    </span>
                  )}
                </div>

                {/* Name */}
                <div className="form-group">
                  <label className="form-label">
                    Full Name <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={submitting}
                    placeholder="e.g. Acme Corporation"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={{ border: errors.name ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.name && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.name}
                    </span>
                  )}
                </div>

                {/* Business Name */}
                <div className="form-group">
                  <label className="form-label">
                    Trading/Business Name
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    disabled={submitting}
                    placeholder="Acme Co."
                    value={businessName}
                    onChange={(e) => setBusinessName(e.target.value)}
                    style={{ border: errors.businessName ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.businessName && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.businessName}
                    </span>
                  )}
                </div>

                {/* Tax ID */}
                <div className="form-group">
                  <label className="form-label">
                    Tax ID / GSTIN Registration
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    disabled={submitting}
                    placeholder="e.g. GST-98765432"
                    value={taxId}
                    onChange={(e) => setTaxId(e.target.value)}
                    style={{ border: errors.taxId ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.taxId && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.taxId}
                    </span>
                  )}
                </div>

                {/* Phone */}
                <div className="form-group">
                  <label className="form-label">
                    Contact Phone
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    disabled={submitting}
                    placeholder="+1 (555) 000-0000"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    style={{ border: errors.phone ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.phone && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.phone}
                    </span>
                  )}
                </div>

                {/* Email */}
                <div className="form-group">
                  <label className="form-label">
                    Billing Email
                  </label>
                  <input
                    type="email"
                    className="form-control"
                    disabled={submitting}
                    placeholder="billing@acme.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    style={{ border: errors.email ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.email && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.email}
                    </span>
                  )}
                </div>

                {/* Credit Limit */}
                <div className="form-group">
                  <label className="form-label">
                    Credit Limit (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    className="form-control"
                    disabled={submitting}
                    placeholder="e.g. 5000.00"
                    value={creditLimit}
                    onChange={(e) => setCreditLimit(e.target.value)}
                    style={{ border: errors.creditLimit ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.creditLimit && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.creditLimit}
                    </span>
                  )}
                </div>

                {/* Price Tier */}
                <div className="form-group">
                  <label className="form-label">
                    Price Tier
                  </label>
                  <select
                    className="form-select"
                    disabled={submitting}
                    value={priceTierId}
                    onChange={(e) => setPriceTierId(e.target.value)}
                  >
                    <option value="">None (Standard Pricing)</option>
                    <option value="1">Tier 1 - VIP (10% Off)</option>
                    <option value="2">Tier 2 - Wholesale (20% Off)</option>
                    <option value="3">Tier 3 - Bulk (5% Off)</option>
                  </select>
                </div>

                {/* Opening Balance */}
                {modalMode === 'create' && (
                  <div className="form-group">
                    <label className="form-label">
                      Opening Balance (₹)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      className="form-control"
                      disabled={submitting}
                      placeholder="0.00"
                      value={openingBalance}
                      onChange={(e) => setOpeningBalance(e.target.value)}
                      style={{ border: errors.openingBalance ? '1px solid var(--danger)' : undefined }}
                    />
                    {errors.openingBalance && (
                      <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                        {errors.openingBalance}
                      </span>
                    )}
                  </div>
                )}

                {/* Status Selection */}
                {modalMode === 'edit' && (
                  <div className="form-group">
                    <label className="form-label">
                      Status
                    </label>
                    <select
                      className="form-select"
                      disabled={submitting}
                      value={statusVal}
                      onChange={(e) => setStatusVal(e.target.value as any)}
                      style={{ border: errors.status ? '1px solid var(--danger)' : undefined }}
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                      <option value="BLACKLISTED">Blacklisted</option>
                    </select>
                    {errors.status && (
                      <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                        {errors.status}
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* Physical Address */}
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label className="form-label">
                  Billing Address
                </label>
                <textarea
                  className="form-control"
                  disabled={submitting}
                  placeholder="123 Main St, Suite 100, City, Country"
                  rows={2}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  style={{ border: errors.address ? '1px solid var(--danger)' : undefined, resize: 'vertical' }}
                />
                {errors.address && (
                  <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    {errors.address}
                  </span>
                )}
              </div>

              {/* Credit Hold Checkbox */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '2rem', backgroundColor: 'rgba(239, 68, 68, 0.03)', border: '1px solid rgba(239, 68, 68, 0.1)', borderRadius: 'var(--border-radius)', padding: '10px 12px', gridColumn: '1 / -1' }}>
                <input
                  type="checkbox"
                  id="hold"
                  disabled={submitting}
                  checked={creditHold}
                  onChange={(e) => setCreditHold(e.target.checked)}
                  style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                />
                <label htmlFor="hold" style={{ fontSize: '0.85rem', color: 'var(--text-primary)', fontWeight: 500, cursor: 'pointer' }}>
                  Place Account on Credit Hold (Blocks future sales invoicing)
                </label>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem', gridColumn: '1 / -1' }}>
                <button type="button" disabled={submitting} onClick={() => setIsModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={submitting} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: '100px', justifyContent: 'center' }}>
                  {submitting ? <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} /> : 'Save Profile'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
};
export default Customers;
