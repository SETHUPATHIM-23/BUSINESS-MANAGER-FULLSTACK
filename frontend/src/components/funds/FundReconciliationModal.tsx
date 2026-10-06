import React, { useState, useEffect } from 'react';
import { X, CheckCircle, AlertCircle } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';

interface FundReconciliationModalProps {
  isOpen: boolean;
  onClose: () => void;
  transactionId: number | null;
  onSaved: () => void;
}

export const FundReconciliationModal: React.FC<FundReconciliationModalProps> = ({ isOpen, onClose, transactionId, onSaved }) => {
  const [formData, setFormData] = useState({
    bankStatementLineRef: ''
  });
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setFormData({ bankStatementLineRef: '' });
      setServerErrors({});
    }
  }, [isOpen]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (serverErrors[name]) {
      setServerErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!transactionId) return;
    
    setSaving(true);
    setServerErrors({});

    try {
      await api.post('/api/funds/reconciliations', {
        fundTransactionId: transactionId,
        bankStatementLineRef: formData.bankStatementLineRef
      });
      toastEvents.success('Reconciliation record created successfully');
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
        toastEvents.error(err.response?.data?.message || 'Failed to create reconciliation record');
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
          <h2 className="modal-title">Create Reconciliation Record</h2>
          <button onClick={onClose} className="close-button"><X size={20} /></button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <p style={{ marginBottom: '1rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            Link transaction #{transactionId} to a bank statement line reference.
          </p>
          <div className="form-group">
            <label className="form-label">Bank Statement Line Reference *</label>
            <input
              type="text"
              name="bankStatementLineRef"
              value={formData.bankStatementLineRef}
              onChange={handleChange}
              className={`form-control ${serverErrors.bankStatementLineRef ? 'error' : ''}`}
              placeholder="e.g. TXN-987654321"
              required
            />
            {serverErrors.bankStatementLineRef && <span className="error-text"><AlertCircle size={14} /> {serverErrors.bankStatementLineRef}</span>}
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <CheckCircle size={18} /> {saving ? 'Processing...' : 'Create Record'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
