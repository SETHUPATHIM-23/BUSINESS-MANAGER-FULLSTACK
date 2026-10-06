import React, { useState, useEffect } from 'react';
import { api, isApiError } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import { X, Plus, Trash2, Save, RefreshCw } from 'lucide-react';

interface PurchaseFormProps {
  poId: number | null;
  onClose: () => void;
  onSave: () => void;
}

interface LineItem {
  id?: number;
  productId: number | '';
  orderedQty: number | '';
  costPrice: number | '';
  taxAmount: number | '';
  lineTotal?: number;
}

interface FormData {
  supplierId: number | '';
  orderDate: string;
  lines: LineItem[];
}

export const PurchaseForm: React.FC<PurchaseFormProps> = ({ poId, onClose, onSave }) => {
  const [formData, setFormData] = useState<FormData>({
    supplierId: '',
    orderDate: new Date().toISOString().split('T')[0],
    lines: [{ productId: '', orderedQty: 1, costPrice: 0, taxAmount: 0 }]
  });

  const [suppliers, setSuppliers] = useState<any[]>([]);
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    const fetchDependencies = async () => {
      try {
        const [suppRes, prodRes] = await Promise.all([
          api.get('/api/suppliers?size=1000'),
          api.get('/api/products?size=1000')
        ]);
        setSuppliers(suppRes.data.content || suppRes.data);
        setProducts(prodRes.data.content || prodRes.data);

        if (poId) {
          const poRes = await api.get(`/api/purchases/${poId}`);
          const data = poRes.data;
          setFormData({
            supplierId: data.supplierId,
            orderDate: data.orderDate,
            lines: data.lines.map((l: any) => ({
              id: l.id,
              productId: l.productId,
              orderedQty: l.orderedQty,
              costPrice: l.costPrice,
              taxAmount: l.taxAmount || 0,
              lineTotal: l.lineTotal
            }))
          });
        }
      } catch (err) {
        toastEvents.error('Failed to load dependencies');
      } finally {
        setInitialLoading(false);
      }
    };
    
    fetchDependencies();
  }, [poId]);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.supplierId) newErrors.supplierId = 'Supplier is required';
    if (!formData.orderDate) newErrors.orderDate = 'Order Date is required';
    if (formData.lines.length === 0) newErrors.lines = 'At least one line is required';

    formData.lines.forEach((line, idx) => {
      if (!line.productId) newErrors[`lines[${idx}].productId`] = 'Product is required';
      if (line.orderedQty === '' || Number(line.orderedQty) <= 0) newErrors[`lines[${idx}].orderedQty`] = 'Quantity must be positive';
      if (line.costPrice === '' || Number(line.costPrice) < 0) newErrors[`lines[${idx}].costPrice`] = 'Cost Price cannot be negative';
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) {
      toastEvents.error('Please fix the errors in the form');
      return;
    }

    setLoading(true);
    setErrors({});
    
    try {
      const payload = {
        supplierId: formData.supplierId,
        orderDate: formData.orderDate,
        lines: formData.lines.map(l => ({
          productId: l.productId,
          orderedQty: l.orderedQty,
          costPrice: l.costPrice,
          taxAmount: l.taxAmount
        }))
      };

      if (poId) {
        await api.put(`/api/purchases/${poId}`, payload);
        toastEvents.success('Purchase order updated successfully');
      } else {
        await api.post('/api/purchases', payload);
        toastEvents.success('Purchase order created successfully');
      }
      onSave();
    } catch (err) {
      if (isApiError(err) && err.response?.data?.fieldErrors) {
        const serverErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach(fe => {
          serverErrors[fe.field] = fe.message;
        });
        setErrors(serverErrors);
        toastEvents.error('Validation failed. Please check your inputs.');
      } else if (isApiError(err) && err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('An unexpected error occurred');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLineChange = (index: number, field: keyof LineItem, value: any) => {
    const newLines = [...formData.lines];
    newLines[index] = { ...newLines[index], [field]: value };
    
    if (field === 'productId' && value !== '') {
       const selectedProd = products.find(p => p.id === Number(value));
       if (selectedProd && (!newLines[index].costPrice || newLines[index].costPrice === 0)) {
           newLines[index].costPrice = selectedProd.costPrice || 0;
       }
    }
    
    setFormData({ ...formData, lines: newLines });
  };

  const addLine = () => {
    setFormData({
      ...formData,
      lines: [...formData.lines, { productId: '', orderedQty: 1, costPrice: 0, taxAmount: 0 }]
    });
  };

  const removeLine = (index: number) => {
    const newLines = [...formData.lines];
    newLines.splice(index, 1);
    setFormData({ ...formData, lines: newLines });
  };

  const calculateSubtotal = () => {
    return formData.lines.reduce((sum, line) => {
      const qty = Number(line.orderedQty) || 0;
      const cost = Number(line.costPrice) || 0;
      return sum + (qty * cost);
    }, 0);
  };

  if (initialLoading) {
    return (
      <div style={{ padding: '3rem', textAlign: 'center' }}>
        <RefreshCw size={24} className="spin" style={{ color: 'var(--accent-color)', margin: '0 auto', opacity: 0.5 }} />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '80vh' }}>
      <div className="modal-header">
        <h2 className="modal-title">{poId ? 'Edit' : 'Create'} Purchase Order</h2>
        <button onClick={onClose} className="close-button">
          <X size={20} />
        </button>
      </div>

      <div className="modal-body">
        <form id="purchaseForm" onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
            <div className="form-group">
              <label className="form-label">Supplier <span style={{ color: 'var(--danger)' }}>*</span></label>
              <select 
                className="form-select"
                value={formData.supplierId}
                onChange={e => setFormData({ ...formData, supplierId: e.target.value === '' ? '' : Number(e.target.value) })}
                style={{ border: errors.supplierId ? '1px solid var(--danger)' : undefined }}
              >
                <option value="">Select a supplier...</option>
                {suppliers.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
              {errors.supplierId && <span style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '0.25rem', display: 'block' }}>{errors.supplierId}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">Order Date <span style={{ color: 'var(--danger)' }}>*</span></label>
              <input 
                type="date"
                className="form-control"
                value={formData.orderDate}
                onChange={e => setFormData({ ...formData, orderDate: e.target.value })}
                style={{ border: errors.orderDate ? '1px solid var(--danger)' : undefined }}
              />
              {errors.orderDate && <span style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '0.25rem', display: 'block' }}>{errors.orderDate}</span>}
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 500 }}>Order Lines</h3>
              <button 
                type="button" 
                onClick={addLine}
                className="btn btn-outline"
                style={{ color: '#3b82f6', border: '1px solid rgba(59, 130, 246, 0.2)', backgroundColor: 'rgba(59, 130, 246, 0.1)' }}
              >
                <Plus size={16} /> Add Line
              </button>
            </div>
            
            {errors.lines && <div style={{ color: 'var(--danger)', fontSize: '0.85rem', marginBottom: '1rem', padding: '0.75rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '6px' }}>{errors.lines}</div>}

            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Product</th>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem', width: '120px' }}>Quantity</th>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem', width: '140px' }}>Unit Cost</th>
                  <th style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--text-secondary)', fontSize: '0.85rem', width: '120px', textAlign: 'right' }}>Total</th>
                  <th style={{ padding: '0.75rem', width: '50px' }}></th>
                </tr>
              </thead>
              <tbody>
                {formData.lines.map((line, idx) => {
                  const qty = Number(line.orderedQty) || 0;
                  const cost = Number(line.costPrice) || 0;
                  const lineTotal = qty * cost;
                  const lineErrProductId = errors[`lines[${idx}].productId`];
                  const lineErrQty = errors[`lines[${idx}].orderedQty`];
                  const lineErrCost = errors[`lines[${idx}].costPrice`];

                  return (
                    <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                      <td style={{ padding: '0.75rem' }}>
                        <select 
                          className="form-select"
                          value={line.productId}
                          onChange={e => handleLineChange(idx, 'productId', e.target.value === '' ? '' : Number(e.target.value))}
                          style={{ border: lineErrProductId ? '1px solid var(--danger)' : undefined, padding: '0.5rem' }}
                        >
                          <option value="">Select product...</option>
                          {products.map(p => (
                            <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                          ))}
                        </select>
                        {lineErrProductId && <div style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '2px' }}>{lineErrProductId}</div>}
                      </td>
                      <td style={{ padding: '0.75rem' }}>
                        <input 
                          type="number" 
                          className="form-control"
                          min="0.0001" 
                          step="any"
                          value={line.orderedQty}
                          onChange={e => handleLineChange(idx, 'orderedQty', e.target.value)}
                          style={{ border: lineErrQty ? '1px solid var(--danger)' : undefined, padding: '0.5rem' }}
                        />
                        {lineErrQty && <div style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '2px' }}>{lineErrQty}</div>}
                      </td>
                      <td style={{ padding: '0.75rem' }}>
                        <div style={{ position: 'relative' }}>
                          <span style={{ position: 'absolute', left: '0.5rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>₹</span>
                          <input 
                            type="number" 
                            className="form-control"
                            min="0" 
                            step="0.01"
                            value={line.costPrice}
                            onChange={e => handleLineChange(idx, 'costPrice', e.target.value)}
                            style={{ border: lineErrCost ? '1px solid var(--danger)' : undefined, padding: '0.5rem 0.5rem 0.5rem 1.5rem' }}
                          />
                        </div>
                        {lineErrCost && <div style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '2px' }}>{lineErrCost}</div>}
                      </td>
                      <td style={{ padding: '0.75rem', textAlign: 'right', fontFamily: 'monospace', fontSize: '1.05rem', color: 'var(--text-primary)' }}>
                        ₹{lineTotal.toFixed(2)}
                      </td>
                      <td style={{ padding: '0.75rem', textAlign: 'center' }}>
                        <button 
                          type="button" 
                          onClick={() => removeLine(idx)}
                          style={{ background: 'none', border: 'none', color: 'var(--danger)', opacity: formData.lines.length > 1 ? 0.7 : 0.3, cursor: formData.lines.length > 1 ? 'pointer' : 'not-allowed' }}
                          disabled={formData.lines.length <= 1}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={3} style={{ padding: '1rem 0.75rem', textAlign: 'right', fontWeight: 500, color: 'var(--text-secondary)' }}>Estimated Subtotal:</td>
                  <td style={{ padding: '1rem 0.75rem', textAlign: 'right', fontWeight: 600, fontSize: '1.1rem', fontFamily: 'monospace', color: 'var(--accent-color)' }}>
                    ₹{calculateSubtotal().toFixed(2)}
                  </td>
                  <td></td>
                </tr>
              </tfoot>
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
          form="purchaseForm"
          disabled={loading}
          className="btn btn-primary"
        >
          {loading ? <RefreshCw size={18} className="spin" /> : <Save size={18} />}
          <span>{poId ? 'Update' : 'Create'} PO</span>
        </button>
      </div>
    </div>
  );
};
