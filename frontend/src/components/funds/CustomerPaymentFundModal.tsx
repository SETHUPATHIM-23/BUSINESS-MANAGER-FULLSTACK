import React, { useState, useEffect } from 'react';
import { X, ArrowDownRight, AlertCircle, UserCheck } from 'lucide-react';
import { api, fetchPaginated } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import type { FundAccountDto } from './FundAccountModal';

interface CustomerPaymentFundModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: FundAccountDto[];
  onSaved: () => void;
}

interface CustomerOption {
  id: number;
  name: string;
  customerCode: string;
}

export const CustomerPaymentFundModal: React.FC<CustomerPaymentFundModalProps> = ({
  isOpen,
  onClose,
  accounts,
  onSaved
}) => {
  const [formData, setFormData] = useState({
    fundAccountId: '',
    customerId: '',
    amount: '',
    transactionDate: new Date().toISOString().split('T')[0],
    description: ''
  });
  const [customers, setCustomers] = useState<CustomerOption[]>([]);
  const [loadingCustomers, setLoadingCustomers] = useState(false);
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setFormData({
        fundAccountId: '',
        customerId: '',
        amount: '',
        transactionDate: new Date().toISOString().split('T')[0],
        description: ''
      });
      setServerErrors({});

      // Fetch customers
      setLoadingCustomers(true);
      fetchPaginated<CustomerOption>('/api/customers', { size: 100 })
        .then(res => setCustomers(res.content || []))
        .catch(() => toastEvents.error('Failed to load customers'))
        .finally(() => setLoadingCustomers(false));
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
      await api.post('/api/funds/transactions/customer-payment', {
        fundAccountId: Number(formData.fundAccountId),
        customerId: formData.customerId ? Number(formData.customerId) : null,
        amount: Number(formData.amount),
        transactionDate: formData.transactionDate,
        description: formData.description
      });
      toastEvents.success('Customer payment credited to fund account successfully');
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
        toastEvents.error(err.response?.data?.message || 'Failed to record customer payment');
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
        <div className="modal-header" style={{ borderBottom: '1px solid rgba(16, 185, 129, 0.2)' }}>
          <h2 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#10b981' }}>
            <ArrowDownRight size={22} /> Register Credited Customer Payment
          </h2>
          <button onClick={onClose} className="close-button"><X size={20} /></button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-group">
            <label className="form-label">Credit to Fund Account *</label>
            <select
              name="fundAccountId"
              value={formData.fundAccountId}
              onChange={handleChange}
              className={`form-control ${serverErrors.fundAccountId ? 'error' : ''}`}
              required
            >
              <option value="">Select receiving cash/bank account...</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} {acc.accountNumber ? `(${acc.accountNumber})` : ''} - Bal: ₹{acc.currentBalance.toFixed(2)}
                </option>
              ))}
            </select>
            {serverErrors.fundAccountId && <span className="error-text"><AlertCircle size={14} /> {serverErrors.fundAccountId}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Customer *</label>
            <select
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              className="form-control"
              disabled={loadingCustomers}
              required
            >
              <option value="">{loadingCustomers ? 'Loading customers...' : 'Select customer...'}</option>
              {customers.map(c => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.customerCode})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Credited Amount (₹) *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              name="amount"
              placeholder="e.g. 5000.00"
              value={formData.amount}
              onChange={handleChange}
              className={`form-control ${serverErrors.amount ? 'error' : ''}`}
              required
            />
            {serverErrors.amount && <span className="error-text"><AlertCircle size={14} /> {serverErrors.amount}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Payment Date *</label>
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
              placeholder="e.g. Cheque / UPI / Bank Transfer ref #"
              className="form-control"
              rows={2}
            />
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-success" disabled={saving} style={{ backgroundColor: '#10b981', borderColor: '#10b981' }}>
              <UserCheck size={18} /> {saving ? 'Recording...' : 'Register Customer Payment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
