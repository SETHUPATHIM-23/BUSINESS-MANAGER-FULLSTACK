import React, { useState, useEffect } from 'react';
import { X, ArrowUpRight, AlertCircle, Building } from 'lucide-react';
import { api, fetchPaginated } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import type { FundAccountDto } from './FundAccountModal';

interface SupplierSettlementFundModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: FundAccountDto[];
  onSaved: () => void;
}

interface SupplierOption {
  id: number;
  name: string;
  supplierCode: string;
}

export const SupplierSettlementFundModal: React.FC<SupplierSettlementFundModalProps> = ({
  isOpen,
  onClose,
  accounts,
  onSaved
}) => {
  const [formData, setFormData] = useState({
    fundAccountId: '',
    supplierId: '',
    amount: '',
    transactionDate: new Date().toISOString().split('T')[0],
    description: ''
  });
  const [suppliers, setSuppliers] = useState<SupplierOption[]>([]);
  const [loadingSuppliers, setLoadingSuppliers] = useState(false);
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setFormData({
        fundAccountId: '',
        supplierId: '',
        amount: '',
        transactionDate: new Date().toISOString().split('T')[0],
        description: ''
      });
      setServerErrors({});

      // Fetch suppliers
      setLoadingSuppliers(true);
      fetchPaginated<SupplierOption>('/api/suppliers', { size: 100 })
        .then(res => setSuppliers(res.content || []))
        .catch(() => toastEvents.error('Failed to load suppliers'))
        .finally(() => setLoadingSuppliers(false));
    }
  }, [isOpen]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (serverErrors[name]) {
      setServerErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setServerErrors({});

    try {
      await api.post('/api/funds/transactions/supplier-settlement', {
        fundAccountId: Number(formData.fundAccountId),
        supplierId: formData.supplierId ? Number(formData.supplierId) : null,
        amount: Number(formData.amount),
        transactionDate: formData.transactionDate,
        description: formData.description
      });
      toastEvents.success('Supplier settlement recorded from fund account successfully');
      onSaved();
      onClose();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const errors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          errors[fe.field] = fe.message;
        });
        setServerErrors(errors);
      } else {
        toastEvents.error(err.response?.data?.message || 'Failed to record supplier settlement');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  const activeAccounts = accounts.filter(a => a.active);

  return (
    <div className="modal-backdrop">
      <div className="modal-content">
        <div className="modal-header" style={{ borderBottom: '1px solid rgba(239, 68, 68, 0.2)' }}>
          <h2 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#ef4444' }}>
            <ArrowUpRight size={22} /> Settle Supplier Payment
          </h2>
          <button onClick={onClose} className="close-button"><X size={20} /></button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-group">
            <label className="form-label">Pay from Fund Account *</label>
            <select
              name="fundAccountId"
              value={formData.fundAccountId}
              onChange={handleChange}
              className={`form-control ${serverErrors.fundAccountId ? 'error' : ''}`}
              required
            >
              <option value="">Select source cash/bank account...</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} {acc.accountNumber ? `(${acc.accountNumber})` : ''} - Bal: ₹{acc.currentBalance.toFixed(2)}
                </option>
              ))}
            </select>
            {serverErrors.fundAccountId && <span className="error-text"><AlertCircle size={14} /> {serverErrors.fundAccountId}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Supplier *</label>
            <select
              name="supplierId"
              value={formData.supplierId}
              onChange={handleChange}
              className="form-control"
              disabled={loadingSuppliers}
              required
            >
              <option value="">{loadingSuppliers ? 'Loading suppliers...' : 'Select supplier...'}</option>
              {suppliers.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.supplierCode})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Settlement Amount (₹) *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              name="amount"
              placeholder="e.g. 10000.00"
              value={formData.amount}
              onChange={handleChange}
              className={`form-control ${serverErrors.amount ? 'error' : ''}`}
              required
            />
            {serverErrors.amount && <span className="error-text"><AlertCircle size={14} /> {serverErrors.amount}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Settlement Date *</label>
            <input
              type="date"
              name="transactionDate"
              value={formData.transactionDate}
              onChange={handleChange}
              className={`form-control ${serverErrors.transactionDate ? 'error' : ''}`}
              required
            />
            {serverErrors.transactionDate && <span className="error-text"><AlertCircle size={14} /> {serverErrors.transactionDate}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Description / Reference</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="e.g. NEFT / Cheque / Purchase Order Settlement #"
              className="form-control"
              rows={2}
            />
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-danger" disabled={saving} style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}>
              <Building size={18} /> {saving ? 'Recording...' : 'Settle Supplier Payment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
