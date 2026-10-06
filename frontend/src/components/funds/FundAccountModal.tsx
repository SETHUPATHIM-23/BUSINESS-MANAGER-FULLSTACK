import React, { useState, useEffect } from 'react';
import { X, Save, AlertCircle } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';

export interface FundAccountDto {
  id: number;
  name: string;
  type: 'CASH' | 'BANK';
  accountNumber: string | null;
  currentBalance: number;
  active: boolean;
  createdAt: string;
}

interface FundAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  account?: FundAccountDto | null;
  onSaved: () => void;
}

export const FundAccountModal: React.FC<FundAccountModalProps> = ({ isOpen, onClose, account, onSaved }) => {
  const [formData, setFormData] = useState({
    name: '',
    type: 'CASH',
    accountNumber: ''
  });
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      if (account) {
        setFormData({
          name: account.name,
          type: account.type,
          accountNumber: account.accountNumber || ''
        });
      } else {
        setFormData({
          name: '',
          type: 'CASH',
          accountNumber: ''
        });
      }
      setServerErrors({});
    }
  }, [isOpen, account]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
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

    // Client-side validation mirroring backend Bean Validation
    const errors: Record<string, string> = {};
    if (!formData.name || formData.name.trim() === '') {
      errors.name = 'Fund account name is required';
    } else if (formData.name.length > 100) {
      errors.name = 'Name must not exceed 100 characters';
    }

    if (formData.type === 'BANK') {
      if (!formData.accountNumber || formData.accountNumber.trim() === '') {
        errors.accountNumber = 'Bank accounts must have an account number';
      } else if (formData.accountNumber.length > 50) {
        errors.accountNumber = 'Account number must not exceed 50 characters';
      }
    }

    if (Object.keys(errors).length > 0) {
      setServerErrors(errors);
      setSaving(false);
      return;
    }

    try {
      const payload = {
        name: formData.name,
        type: formData.type,
        accountNumber: formData.type === 'BANK' ? formData.accountNumber : null
      };

      if (account) {
        await api.put(`/api/funds/accounts/${account.id}`, { ...payload, active: account.active });
        toastEvents.success('Fund account updated successfully');
      } else {
        await api.post('/api/funds/accounts', payload);
        toastEvents.success('Fund account created successfully');
      }
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
        toastEvents.error(err.response?.data?.message || 'Failed to save fund account');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop">
      <div className="modal-content">
        <div className="modal-header">
          <h2 className="modal-title">{account ? 'Edit Fund Account' : 'New Fund Account'}</h2>
          <button onClick={onClose} className="close-button"><X size={20} /></button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-group">
            <label className="form-label">Account Name *</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              className={`form-control ${serverErrors.name ? 'error' : ''}`}
              required
            />
            {serverErrors.name && <span className="error-text"><AlertCircle size={14} /> {serverErrors.name}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Account Type *</label>
            <select
              name="type"
              value={formData.type}
              onChange={handleChange}
              className="form-control"
              required
            >
              <option value="CASH">Cash Account</option>
              <option value="BANK">Bank Account</option>
            </select>
          </div>

          {formData.type === 'BANK' && (
            <div className="form-group">
              <label className="form-label">Account Number *</label>
              <input
                type="text"
                name="accountNumber"
                value={formData.accountNumber}
                onChange={handleChange}
                className={`form-control ${serverErrors.accountNumber ? 'error' : ''}`}
                required
              />
              {serverErrors.accountNumber && <span className="error-text"><AlertCircle size={14} /> {serverErrors.accountNumber}</span>}
              {serverErrors.bankRequiresAccountNumber && <span className="error-text"><AlertCircle size={14} /> {serverErrors.bankRequiresAccountNumber}</span>}
            </div>
          )}

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={18} /> {saving ? 'Saving...' : 'Save Account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
