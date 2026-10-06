import React, { useState, useEffect } from 'react';
import { api, isApiError } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import { X, Save, RefreshCw, AlertTriangle, CheckCircle } from 'lucide-react';

interface ThreeWayMatchFormProps {
  poId: number;
  onClose: () => void;
  onSuccess: () => void;
}

export const ThreeWayMatchForm: React.FC<ThreeWayMatchFormProps> = ({ poId, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [poDetails, setPoDetails] = useState<any>(null);
  
  const [supplierInvoicedSubtotal, setSupplierInvoicedSubtotal] = useState<number | ''>('');
  const [freightAmount, setFreightAmount] = useState<number>(0);
  const [dutiesAmount, setDutiesAmount] = useState<number>(0);
  const [forceApprove, setForceApprove] = useState(false);
  const [mismatchError, setMismatchError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPO = async () => {
      try {
        const res = await api.get(`/api/purchases/${poId}`);
        setPoDetails(res.data);
        setSupplierInvoicedSubtotal(res.data.totalAmount || 0);
      } catch (err) {
        toastEvents.error('Failed to load purchase order details');
        onClose();
      } finally {
        setLoading(false);
      }
    };
    
    fetchPO();
  }, [poId, onClose]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setMismatchError(null);
    
    try {
      const payload = {
        purchaseOrderId: poId,
        supplierInvoicedSubtotal: Number(supplierInvoicedSubtotal) || 0,
        freightAmount,
        dutiesAmount,
        forceApprove
      };

      await api.post(`/api/purchases/${poId}/invoice`, payload);
      toastEvents.success('Supplier invoice posted successfully. Landed costs allocated and MAC adjusted.');
      onSuccess();
    } catch (err) {
      if (isApiError(err) && err.response?.status === 409) {
        setMismatchError(err.response?.data?.message || 'Invoice mismatch exceeds tolerance threshold.');
      } else if (isApiError(err) && err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('Failed to process supplier invoice');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '3rem', textAlign: 'center' }}>
        <RefreshCw size={24} className="spin" style={{ color: 'var(--accent-color)', margin: '0 auto', opacity: 0.5 }} />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '80vh' }}>
      <div className="modal-header">
        <div>
          <h2 className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckCircle size={22} style={{ color: 'var(--success)' }} />
            Three-Way Match & Invoice Posting
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            PO: {poDetails?.poNumber} | {poDetails?.supplierName}
          </p>
        </div>
        <button onClick={onClose} className="close-button">
          <X size={20} />
        </button>
      </div>

      <div className="modal-body">
        <form id="invoiceForm" onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
            <div className="form-group">
              <label className="form-label">Supplier Invoice Subtotal <span style={{ color: 'var(--danger)' }}>*</span></label>
              <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>₹</span>
                <input 
                  type="number" 
                  className="form-control"
                  step="0.01"
                  required
                  value={supplierInvoicedSubtotal}
                  onChange={e => setSupplierInvoicedSubtotal(e.target.value === '' ? '' : Number(e.target.value))}
                  style={{ paddingLeft: '1.75rem' }}
                />
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Original PO Total: ₹{poDetails?.totalAmount?.toFixed(2)}</p>
            </div>

            <div className="form-group">
              <label className="form-label">Freight Amount</label>
              <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>₹</span>
                <input 
                  type="number" 
                  className="form-control"
                  step="0.01"
                  value={freightAmount}
                  onChange={e => setFreightAmount(Number(e.target.value) || 0)}
                  style={{ paddingLeft: '1.75rem' }}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Duties & Customs</label>
              <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>₹</span>
                <input 
                  type="number" 
                  className="form-control"
                  step="0.01"
                  value={dutiesAmount}
                  onChange={e => setDutiesAmount(Number(e.target.value) || 0)}
                  style={{ paddingLeft: '1.75rem' }}
                />
              </div>
            </div>
          </div>

          {mismatchError && (
            <div className="glass-panel" style={{ padding: '1rem', border: '1px solid var(--danger)', backgroundColor: 'rgba(239, 68, 68, 0.1)', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--danger)', fontWeight: 500 }}>
                <AlertTriangle size={18} />
                <span>Three-Way Match Discrepancy Found</span>
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>{mismatchError}</p>
              
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginTop: '0.5rem', userSelect: 'none' }}>
                <input 
                  type="checkbox" 
                  checked={forceApprove} 
                  onChange={e => setForceApprove(e.target.checked)}
                  style={{ width: '16px', height: '16px' }}
                />
                <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-primary)' }}>Force Approve Discrepancy (Requires Admin/Supervisor validation)</span>
              </label>
            </div>
          )}

          <div className="glass-panel" style={{ padding: '1rem', backgroundColor: 'rgba(0,0,0,0.1)' }}>
            <h4 style={{ fontWeight: 500, marginBottom: '0.5rem', fontSize: '0.95rem' }}>Landed Cost Calculation Preview:</h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', marginBottom: '0.25rem' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Invoice Subtotal:</span>
              <span style={{ fontFamily: 'monospace' }}>₹{(Number(supplierInvoicedSubtotal) || 0).toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', marginBottom: '0.25rem' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Freight & Duties:</span>
              <span style={{ fontFamily: 'monospace' }}>₹{(freightAmount + dutiesAmount).toFixed(2)}</span>
            </div>
            <div style={{ borderTop: '1px solid var(--border-color)', margin: '0.5rem 0', paddingTop: '0.5rem', display: 'flex', justifyContent: 'space-between', fontWeight: 600 }}>
              <span>Total Landed Cost:</span>
              <span style={{ color: 'var(--accent-color)', fontFamily: 'monospace' }}>₹{((Number(supplierInvoicedSubtotal) || 0) + freightAmount + dutiesAmount).toFixed(2)}</span>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              * Landed costs will be allocated proportionally across the received items and integrated into the Moving Average Cost (MAC) of inventory.
            </p>
          </div>

        </form>
      </div>

      <div className="modal-footer">
        <button 
          type="button" 
          onClick={onClose}
          className="btn btn-secondary"
        >
          Cancel
        </button>
        <button 
          type="submit" 
          form="invoiceForm"
          disabled={Boolean(submitting || (mismatchError && !forceApprove))}
          className="btn"
          style={{ 
            display: 'flex', alignItems: 'center', gap: '0.5rem',
            backgroundColor: mismatchError ? 'var(--warning)' : 'var(--success)', 
            color: '#fff' 
          }}
        >
          {submitting ? <RefreshCw size={18} className="spin" /> : <Save size={18} />}
          <span>{mismatchError ? 'Force Post Invoice' : 'Verify & Post Invoice'}</span>
        </button>
      </div>
    </div>
  );
};
