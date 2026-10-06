import React, { useState, useEffect } from 'react';
import { X, Send, AlertCircle } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import type { FundAccountDto } from './FundAccountModal';

interface FundTransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: FundAccountDto[];
  onSaved: () => void;
}

export const FundTransferModal: React.FC<FundTransferModalProps> = ({ isOpen, onClose, accounts, onSaved }) => {
  const [formData, setFormData] = useState({
    sourceAccountId: '',
    targetAccountId: '',
    amount: '',
    transactionDate: new Date().toISOString().split('T')[0],
    description: ''
  });
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setFormData({
        sourceAccountId: '',
        targetAccountId: '',
        amount: '',
        transactionDate: new Date().toISOString().split('T')[0],
        description: ''
      });
      setServerErrors({});
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

    if (formData.sourceAccountId === formData.targetAccountId) {
      setServerErrors({ targetAccountId: 'Target account must be different from source account' });
      setSaving(false);
      return;
    }

    try {
      await api.post('/api/funds/transactions/transfer', {
        sourceAccountId: Number(formData.sourceAccountId),
        targetAccountId: Number(formData.targetAccountId),
        amount: Number(formData.amount),
        transactionDate: formData.transactionDate,
        description: formData.description
      });
      toastEvents.success('Funds transferred successfully');
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
        toastEvents.error(err.response?.data?.message || 'Failed to transfer funds');
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
        <div className="modal-header">
          <h2 className="modal-title">Transfer Funds</h2>
          <button onClick={onClose} className="close-button"><X size={20} /></button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-group">
            <label className="form-label">Source Account *</label>
            <select
              name="sourceAccountId"
              value={formData.sourceAccountId}
              onChange={handleChange}
              className={`form-control ${serverErrors.sourceAccountId ? 'error' : ''}`}
              required
            >
              <option value="">Select source account...</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} {acc.accountNumber ? `(${acc.accountNumber})` : ''} - Bal: ₹{acc.currentBalance.toFixed(2)}
                </option>
              ))}
            </select>
            {serverErrors.sourceAccountId && <span className="error-text"><AlertCircle size={14} /> {serverErrors.sourceAccountId}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Target Account *</label>
            <select
              name="targetAccountId"
              value={formData.targetAccountId}
              onChange={handleChange}
              className={`form-control ${serverErrors.targetAccountId ? 'error' : ''}`}
              required
            >
              <option value="">Select target account...</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} {acc.accountNumber ? `(${acc.accountNumber})` : ''}
                </option>
              ))}
            </select>
            {serverErrors.targetAccountId && <span className="error-text"><AlertCircle size={14} /> {serverErrors.targetAccountId}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Amount *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              name="amount"
              value={formData.amount}
              onChange={handleChange}
              className={`form-control ${serverErrors.amount ? 'error' : ''}`}
              required
            />
            {serverErrors.amount && <span className="error-text"><AlertCircle size={14} /> {serverErrors.amount}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Transaction Date *</label>
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
            <label className="form-label">Description (Optional)</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              className="form-control"
              rows={2}
            />
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Send size={18} /> {saving ? 'Processing...' : 'Transfer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
