import React, { useState, useEffect } from 'react';
import { api } from '../../../utils/api';
import { toastEvents } from '../../../utils/toast';
import { X, CheckCircle, RefreshCw } from 'lucide-react';
import type { InvoiceResponseDto } from './InvoiceSummaryPanel';
import type { InvoiceLineResponseDto } from './LineItemEditor';

interface ReturnInvoiceModalProps {
  invoiceId: number;
  onClose: () => void;
  onSuccess: () => void;
}

export const ReturnInvoiceModal: React.FC<ReturnInvoiceModalProps> = ({ invoiceId, onClose, onSuccess }) => {
  const [invoice, setInvoice] = useState<InvoiceResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  // Map of lineId -> quantity to return
  const [returnQuantities, setReturnQuantities] = useState<Record<number, string>>({});

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const res = await api.get(`/api/billings/${invoiceId}`);
        const inv: InvoiceResponseDto = res.data;
        setInvoice(inv);
        
        // Initialize return quantities to 0
        const initialQs: Record<number, string> = {};
        inv.lines?.forEach((line: InvoiceLineResponseDto) => {
          if (line.id) initialQs[line.id] = '0';
        });
        setReturnQuantities(initialQs);
      } catch (err) {
        toastEvents.error('Failed to fetch invoice details for return');
        onClose();
      } finally {
        setLoading(false);
      }
    };
    fetchInvoice();
  }, [invoiceId, onClose]);

  const handleQtyChange = (lineId: number, val: string, maxQty: number) => {
    // Basic validation
    const num = parseFloat(val);
    if (!isNaN(num) && num > maxQty) {
      toastEvents.warning(`Cannot return more than originally billed (${maxQty})`);
      setReturnQuantities(prev => ({ ...prev, [lineId]: maxQty.toString() }));
      return;
    }
    if (!isNaN(num) && num < 0) {
      setReturnQuantities(prev => ({ ...prev, [lineId]: '0' }));
      return;
    }
    setReturnQuantities(prev => ({ ...prev, [lineId]: val }));
  };

  const handleSubmit = async () => {
    // Filter out 0 quantities
    const payloadMap: Record<number, number> = {};
    let hasReturns = false;
    
    Object.entries(returnQuantities).forEach(([lineIdStr, qtyStr]) => {
      const qty = parseFloat(qtyStr);
      if (!isNaN(qty) && qty > 0) {
        payloadMap[parseInt(lineIdStr)] = qty;
        hasReturns = true;
      }
    });

    if (!hasReturns) {
      toastEvents.error('Please specify at least one item quantity to return');
      return;
    }

    setSubmitting(true);
    try {
      await api.post(`/api/billings/${invoiceId}/return`, {
        returnedQuantities: payloadMap
      });
      toastEvents.success('Credit Note processed successfully');
      onSuccess();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to process return');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="modal-backdrop">
        <div className="modal-content" style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}>
          <RefreshCw size={24} style={{ animation: 'spin 1s linear infinite', color: 'var(--accent-primary)' }} />
        </div>
      </div>
    );
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-content" style={{ maxWidth: '700px' }}>
        <div className="modal-header">
          <h2 className="modal-title">Process Return for {invoice?.invoiceNumber}</h2>
          <button onClick={onClose} className="modal-close-btn" disabled={submitting}>
            <X size={20} />
          </button>
        </div>
        
        <div style={{ padding: '1.5rem' }}>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem', fontSize: '0.9rem' }}>
            Specify the quantities to return for each line item. A Credit Note will be generated.
          </p>

          <div style={{ backgroundColor: '#1e293b', borderRadius: '8px', border: '1px solid var(--border-color)', overflow: 'hidden', marginBottom: '1.5rem' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderBottom: '1px solid var(--border-color)' }}>
                <tr>
                  <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Product</th>
                  <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Billed Qty</th>
                  <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Rate</th>
                  <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600, width: '140px' }}>Return Qty</th>
                </tr>
              </thead>
              <tbody>
                {invoice?.lines?.map((line: InvoiceLineResponseDto) => (
                  <tr key={line.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={{ padding: '1rem', color: '#fff' }}>
                      <div style={{ fontWeight: 500 }}>{line.productName}</div>
                    </td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{line.quantity}</td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>₹{line.unitPrice?.toFixed(2)}</td>
                    <td style={{ padding: '0.5rem 1rem' }}>
                      <input 
                        type="number"
                        min="0"
                        max={line.quantity}
                        step="any"
                        className="form-control"
                        value={line.id ? returnQuantities[line.id] || '' : ''}
                        onChange={(e) => line.id && handleQtyChange(line.id, e.target.value, line.quantity)}
                        disabled={submitting}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
            <button type="button" onClick={onClose} disabled={submitting} className="btn btn-secondary">
              Cancel
            </button>
            <button 
              type="button" 
              onClick={handleSubmit} 
              disabled={submitting} 
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              {submitting ? 'Processing...' : (
                <>
                  <CheckCircle size={18} /> Generate Credit Note
                </>
              )}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
