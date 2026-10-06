import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { 
  Plus, Search, Edit, Trash2, UserX, ChevronLeft, ChevronRight, 
  ArrowUpDown, SlidersHorizontal, AlertCircle, RefreshCw, X, FileText, Truck
} from 'lucide-react';

interface SupplierSummary {
  id: number;
  supplierCode: string;
  name: string;
  businessName?: string;
  phone?: string;
  email?: string;
  paymentTermsDays: number;
  status: 'ACTIVE' | 'INACTIVE';
}

export const Suppliers: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('SUPPLIER_WRITE');
  const navigate = useNavigate();

  // List & Filter States
  const [suppliers, setSuppliers] = useState<SupplierSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  
  // Pagination & Sorting States
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('supplierCode');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  // Modal & Form States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedSupplierId, setSelectedSupplierId] = useState<number | null>(null);
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
  const [paymentTermsDays, setPaymentTermsDays] = useState('0');
  const [bankAccountDetails, setBankAccountDetails] = useState('');
  const [openingBalance, setOpeningBalance] = useState('0.00');
  const [statusVal, setStatusVal] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');

  const fetchSuppliers = async () => {
    setLoading(true);
    try {
      const params: any = {
        page,
        size: 10,
        sort: `${sortField},${sortOrder}`
      };

      if (searchQuery.trim()) {
        // Simple heuristic: if query contains numbers or dashes, search by code, else by name
        if (/[\d-]/.test(searchQuery)) {
          params.code = searchQuery.trim();
        } else {
          params.name = searchQuery.trim();
        }
      }

      if (statusFilter) {
        params.status = statusFilter;
      }

      const response = await fetchPaginated<SupplierSummary>('/api/suppliers', params);
      setSuppliers(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to retrieve suppliers list', err);
      toastEvents.error('Failed to load supplier records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers();
  }, [page, sortField, sortOrder, statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchSuppliers();
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

  const handleDeactivate = async (id: number) => {
    if (!window.confirm('Are you sure you want to deactivate this supplier record?')) return;
    try {
      await api.put(`/api/suppliers/${id}/deactivate`);
      toastEvents.success('Supplier deactivated successfully');
      fetchSuppliers();
    } catch (err: any) {
      console.error('Deactivation failed', err);
      toastEvents.error(err.response?.data?.message || 'Deactivation failed.');
    }
  };

  const openCreateModal = () => {
    setModalMode('create');
    setSelectedSupplierId(null);
    setErrors({});
    setCode('');
    setName('');
    setBusinessName('');
    setPhone('');
    setEmail('');
    setAddress('');
    setTaxId('');
    setPaymentTermsDays('0');
    setBankAccountDetails('');
    setOpeningBalance('0.00');
    setStatusVal('ACTIVE');
    setIsModalOpen(true);
  };

  const openEditModal = async (id: number) => {
    setModalMode('edit');
    setSelectedSupplierId(id);
    setErrors({});
    setLoading(true);
    try {
      const response = await api.get(`/api/suppliers/${id}`);
      const data = response.data;
      setCode(data.supplierCode);
      setName(data.name);
      setBusinessName(data.businessName || '');
      setPhone(data.phone || '');
      setEmail(data.email || '');
      setAddress(data.address || '');
      setTaxId(data.taxId || '');
      setPaymentTermsDays(data.paymentTermsDays.toString());
      setBankAccountDetails(data.bankAccountDetails || '');
      setOpeningBalance(data.openingBalance.toString());
      setStatusVal(data.status);
      setIsModalOpen(true);
    } catch (err) {
      console.error('Failed to load supplier details', err);
      toastEvents.error('Failed to retrieve supplier profile details.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    // Client-side validations
    const clientErrors: Record<string, string> = {};
    if (!name.trim()) clientErrors.name = 'Name is required';
    if (modalMode === 'create' && !code.trim()) clientErrors.supplierCode = 'Supplier code is required';
    if (email.trim() && !/\S+@\S+\.\S+/.test(email)) clientErrors.email = 'Invalid email format';
    if (paymentTermsDays && (parseInt(paymentTermsDays) < 0)) clientErrors.paymentTermsDays = 'Payment terms must be positive or zero';
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
        paymentTermsDays: parseInt(paymentTermsDays) || 0,
        bankAccountDetails,
        status: statusVal
      };

      if (modalMode === 'create') {
        payload.supplierCode = code;
        payload.openingBalance = parseFloat(openingBalance) || 0;

        // Optimistic UI update
        const optimisticSupplier: SupplierSummary = {
          id: Math.random(),
          supplierCode: code,
          name,
          businessName,
          phone,
          email,
          paymentTermsDays: parseInt(paymentTermsDays) || 0,
          status: statusVal
        };
        setSuppliers((prev) => [optimisticSupplier, ...prev]);

        await api.post('/api/suppliers', payload);
        toastEvents.success('Supplier profile created successfully');
      } else {
        // Optimistic UI update
        setSuppliers((prev) =>
          prev.map((s) =>
            s.id === selectedSupplierId
              ? {
                  ...s,
                  name,
                  businessName,
                  phone,
                  email,
                  paymentTermsDays: parseInt(paymentTermsDays) || 0,
                  status: statusVal
                }
              : s
          )
        );

        await api.put(`/api/suppliers/${selectedSupplierId}`, payload);
        toastEvents.success('Supplier profile updated successfully');
      }
      setIsModalOpen(false);
      fetchSuppliers();
    } catch (err: any) {
      console.error('Save failed', err);
      if (err.response?.status === 400 && err.response?.data?.fieldErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          backendErrors[fe.field] = fe.message;
        });
        setErrors(backendErrors);
        toastEvents.error('Validation failed. Please correct form fields.');
      } else if (err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('An unexpected error occurred.');
      }
      // Revert optimistic update
      fetchSuppliers();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number, code: string) => {
    if (!window.confirm(`Are you sure you want to hard-delete supplier ${code}? This action is irreversible.`)) return;
    try {
      await api.delete(`/api/suppliers/${id}`);
      toastEvents.success('Supplier record deleted successfully');
      fetchSuppliers();
    } catch (err: any) {
      console.error('Deletion failed', err);
      toastEvents.error(err.response?.data?.message || 'Cannot delete supplier with transaction history.');
    }
  };

  return (
    <div className="page-container">
      {/* Header section */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Truck size={22} />
          </div>
          <div>
            <h1 className="page-title">Supplier Directory</h1>
            <p className="page-description">
              Manage inventory vendors, contact details, payment terms, and outstanding ledger account balances
            </p>
          </div>
        </div>
        <div className="header-actions">
          {hasWriteAccess && (
            <button 
              className="btn btn-primary" 
              onClick={openCreateModal}
            >
              <Plus size={18} />
              <span>Add Supplier</span>
            </button>
          )}
        </div>
      </div>

      {/* Filter panel */}
      <div className="filters-panel">
        <form onSubmit={handleSearchSubmit} className="filters-group">
          <div className="search-input-wrapper">
            <Search size={16} className="search-icon" />
            <input
              type="text"
              className="search-input"
              placeholder="Search by vendor name or code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-secondary">
            Search
          </button>
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
          </select>
        </div>
      </div>

      {/* Listing table */}
      <div className="table-wrapper">
        {loading && suppliers.length === 0 ? (
          <div style={{ padding: '4rem', textAlign: 'center' }}>
            <RefreshCw size={24} style={{ animation: 'spin 1s linear infinite', color: 'var(--accent-primary)', marginBottom: '1rem' }} />
            <p style={{ color: 'var(--text-secondary)' }}>Retrieving supplier records...</p>
          </div>
        ) : suppliers.length === 0 ? (
          <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <AlertCircle size={32} style={{ marginBottom: '1rem' }} />
            <p>No supplier records found matching the filters.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr className="table-header-row">
                  <th onClick={() => handleSort('supplierCode')}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span>Code</span>
                      <ArrowUpDown size={12} />
                    </div>
                  </th>
                  <th onClick={() => handleSort('name')}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span>Vendor Name</span>
                      <ArrowUpDown size={12} />
                    </div>
                  </th>
                  <th>Contact</th>
                  <th>Terms</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {suppliers.map((s) => (
                  <tr key={s.id} className="table-row">
                    <td style={{ fontWeight: 600 }}>{s.supplierCode}</td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <span style={{ fontWeight: 500 }}>{s.name}</span>
                        {s.businessName && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>{s.businessName}</span>}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                        {s.phone && <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{s.phone}</span>}
                        {s.email && <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{s.email}</span>}
                      </div>
                    </td>
                    <td>
                      {s.paymentTermsDays} Days
                    </td>
                    <td>
                      <span className={s.status === 'ACTIVE' ? 'badge badge-success' : 'badge badge-neutral'}>
                        {s.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                        <button onClick={() => navigate(`/suppliers/${s.id}/statement`)} className="btn btn-outline btn-sm" title="Supplier Statement">
                          <FileText size={16} />
                        </button>
                        
                        {hasWriteAccess && (
                          <>
                            <button onClick={() => openEditModal(s.id)} className="btn btn-outline btn-sm" title="Edit Supplier">
                              <Edit size={16} />
                            </button>
                            {s.status === 'ACTIVE' && (
                              <button onClick={() => handleDeactivate(s.id)} className="btn btn-outline btn-sm" title="Deactivate Supplier">
                                <UserX size={16} />
                              </button>
                            )}
                            <button onClick={() => handleDelete(s.id, s.supplierCode)} className="btn btn-danger btn-sm" title="Delete Supplier">
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
              Showing Page {page + 1} of {totalPages} ({totalElements} total records)
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button 
                onClick={() => setPage(p => Math.max(0, p - 1))} 
                disabled={page === 0} 
                className="btn btn-secondary" 
                style={{ opacity: page === 0 ? 0.4 : 1 }}
              >
                <ChevronLeft size={16} />
              </button>
              <button 
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} 
                disabled={page === totalPages - 1} 
                className="btn btn-secondary" 
                style={{ opacity: page === totalPages - 1 ? 0.4 : 1 }}
              >
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
                {modalMode === 'create' ? 'Create New Supplier' : 'Edit Supplier Profile'}
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
                    Supplier Code <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    disabled={modalMode === 'edit' || submitting}
                    placeholder="e.g. SUPP-001"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    style={{
                      border: errors.supplierCode ? '1px solid var(--danger)' : undefined,
                      opacity: modalMode === 'edit' ? 0.6 : 1
                    }}
                  />
                  {errors.supplierCode && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.supplierCode}
                    </span>
                  )}
                </div>

                {/* Name */}
                <div className="form-group">
                  <label className="form-label">
                    Supplier Name <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    disabled={submitting}
                    placeholder="e.g. Global Tech Supplies"
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
                    placeholder="Global Tech"
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
                    placeholder="e.g. TAX-4433221"
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
                    placeholder="+1 (555) 123-4567"
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
                    Sales Email
                  </label>
                  <input
                    type="email"
                    className="form-control"
                    disabled={submitting}
                    placeholder="sales@globaltech.com"
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

                {/* Payment Terms Days */}
                <div className="form-group">
                  <label className="form-label">
                    Payment Terms (Days)
                  </label>
                  <input
                    type="number"
                    min="0"
                    className="form-control"
                    disabled={submitting}
                    placeholder="0"
                    value={paymentTermsDays}
                    onChange={(e) => setPaymentTermsDays(e.target.value)}
                    style={{ border: errors.paymentTermsDays ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.paymentTermsDays && (
                    <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                      {errors.paymentTermsDays}
                    </span>
                  )}
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
                  Supplier Address
                </label>
                <textarea
                  className="form-control"
                  disabled={submitting}
                  placeholder="456 Vendor Blvd, Suite 200, City, Country"
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

              {/* Bank Account Details (Encrypted at rest) */}
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label className="form-label">
                  Bank Account Details (Encrypted at rest)
                </label>
                <textarea
                  className="form-control"
                  disabled={submitting}
                  placeholder="e.g. Routing: 123456789, Account: 987654321, Bank: Chase"
                  rows={2}
                  value={bankAccountDetails}
                  onChange={(e) => setBankAccountDetails(e.target.value)}
                  style={{ border: errors.bankAccountDetails ? '1px solid var(--danger)' : undefined, resize: 'vertical' }}
                />
                {errors.bankAccountDetails && (
                  <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>
                    {errors.bankAccountDetails}
                  </span>
                )}
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
export default Suppliers;
