import React, { useState, useEffect } from 'react';
import { api, isApiError } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import { X, Save, RefreshCw, Package } from 'lucide-react';

interface GoodsReceiptFormProps {
  poId: number;
  onClose: () => void;
  onSuccess: () => void;
}

export const GoodsReceiptForm: React.FC<GoodsReceiptFormProps> = ({ poId, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [poDetails, setPoDetails] = useState<any>(null);
  
  const [receivedDate, setReceivedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [receiveLines, setReceiveLines] = useState<Record<number, number | ''>>({});

  useEffect(() => {
    const fetchPO = async () => {
      try {
        const res = await api.get(`/api/purchases/${poId}`);
        setPoDetails(res.data);
        
        const initialLines: Record<number, number> = {};
        res.data.lines.forEach((line: any) => {
          initialLines[line.id] = 0; 
        });
        setReceiveLines(initialLines);
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
    
    try {
      const payload = {
        poId,
        receivedDate,
        lines: Object.entries(receiveLines)
          .map(([purchaseLineId, receivedQty]) => ({
            purchaseLineId: Number(purchaseLineId),
            receivedQty: Number(receivedQty) || 0
          }))
          .filter(line => line.receivedQty > 0) 
      };
      
      if (payload.lines.length === 0) {
        toastEvents.error('You must receive a quantity > 0 for at least one line');
        setSubmitting(false);
        return;
      }

      await api.post(`/api/purchases/${poId}/receive`, payload);
      toastEvents.success('Goods receipt recorded successfully. Inventory updated.');
      onSuccess();
    } catch (err) {
      if (isApiError(err) && err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('Failed to record goods receipt');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleSetAllToMax = () => {
    if (!poDetails) return;
    const newLines: Record<number, number> = {};
    poDetails.lines.forEach((line: any) => {
      newLines[line.id] = line.orderedQty;
    });
    setReceiveLines(newLines);
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
            <Package size={22} style={{ color: 'var(--accent-primary)' }} />
            Receive Goods
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            Purchase Order: {poDetails?.poNumber} | {poDetails?.supplierName}
          </p>
        </div>
        <button onClick={onClose} className="close-button">
          <X size={20} />
        </button>
      </div>

      <div className="modal-body">
        <form id="receiptForm" onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div className="form-group" style={{ maxWidth: '200px' }}>
            <label className="form-label">Receipt Date <span style={{ color: 'var(--danger)' }}>*</span></label>
            <input 
              type="date"
              className="form-control"
              value={receivedDate}
              onChange={e => setReceivedDate(e.target.value)}
              required
            />
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 500 }}>Items to Receive</h3>
              <button 
                type="button" 
                onClick={handleSetAllToMax}
                style={{ fontSize: '0.85rem', color: 'var(--accent-color)', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}
              >
                Set All to Ordered Qty
              </button>
            </div>

            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Product</th>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem', width: '120px' }}>Ordered Qty</th>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem', width: '150px' }}>Receiving Now</th>
                </tr>
              </thead>
              <tbody>
                {poDetails?.lines.map((line: any) => (
                  <tr key={line.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={{ padding: '0.75rem' }}>
                      <div style={{ fontWeight: 500 }}>{line.productName}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{line.productSku}</div>
                    </td>
                    <td style={{ padding: '0.75rem', color: 'var(--text-primary)', fontFamily: 'monospace', fontSize: '1.05rem' }}>
                      {line.orderedQty}
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      <input 
                        type="number" 
                        className="form-control"
                        min="0"
                        step="any"
                        value={receiveLines[line.id] === undefined ? '' : receiveLines[line.id]}
                        onChange={e => {
                          const val = e.target.value === '' ? '' : Number(e.target.value);
                          setReceiveLines(prev => ({ ...prev, [line.id]: val }));
                        }}
                        style={{ fontFamily: 'monospace', fontSize: '1.05rem', padding: '0.5rem' }}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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
          form="receiptForm"
          disabled={submitting}
          className="btn btn-primary"
          style={{ backgroundColor: 'var(--success)' }}
        >
          {submitting ? <RefreshCw size={18} className="spin" /> : <Save size={18} />}
          <span>Confirm Receipt</span>
        </button>
      </div>
    </div>
  );
};
