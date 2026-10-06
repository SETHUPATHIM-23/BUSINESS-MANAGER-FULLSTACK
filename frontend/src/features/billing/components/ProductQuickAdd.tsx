import React, { useState, useEffect } from 'react';
import { api } from '../../../utils/api';
import { Plus } from 'lucide-react';

export interface ProductLookup {
  id: number;
  sku: string;
  name: string;
  baseSellingPrice: number;
}

export interface ProductQuickAddProps {
  onAdd: (product: ProductLookup, quantity: number, rate: number) => void;
  disabled?: boolean;
}

export const ProductQuickAdd: React.FC<ProductQuickAddProps> = ({ onAdd, disabled = false }) => {
  const [products, setProducts] = useState<ProductLookup[]>([]);
  const [loading, setLoading] = useState(true);
  
  const [selectedProductId, setSelectedProductId] = useState<string>('');
  const [quantityStr, setQuantityStr] = useState<string>('1');
  const [rateStr, setRateStr] = useState<string>('');

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const res = await api.get('/api/products', { params: { size: 1000 } });
        setProducts(res.data.content || []);
      } catch (err) {
        console.error('Failed to load products', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
  }, []);

  const selectedProduct = products.find(p => p.id.toString() === selectedProductId);

  // Auto-populate rate when product changes
  useEffect(() => {
    if (selectedProduct && !rateStr) {
      setRateStr(selectedProduct.baseSellingPrice.toString());
    }
  }, [selectedProduct, rateStr]);

  const handleAdd = () => {
    if (!selectedProduct) return;
    
    const qty = parseFloat(quantityStr) || 0;
    const rate = parseFloat(rateStr) || 0;
    
    if (qty <= 0) return;
    
    onAdd(selectedProduct, qty, rate);
    
    // Reset form
    setSelectedProductId('');
    setQuantityStr('1');
    setRateStr('');
  };

  if (loading) {
    return <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Loading products...</div>;
  }

  return (
    <div style={{ padding: '1rem', borderRadius: '8px', backgroundColor: 'rgba(255,255,255,0.02)', border: '1px solid var(--border-color)', marginBottom: '1.5rem' }}>
      <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        Quick Add Product
      </h4>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Select Product</label>
          <select 
            className="form-select"
            value={selectedProductId}
            onChange={(e) => {
              setSelectedProductId(e.target.value);
              setRateStr(''); // Clear rate so it auto-populates
            }}
            disabled={disabled}
          >
            <option value="">-- Choose Product --</option>
            {products.map(p => (
              <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>
            ))}
          </select>
        </div>
        
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Rate</label>
          <input
            type="number"
            className="form-control"
            min="0"
            step="0.01"
            value={rateStr}
            onChange={(e) => setRateStr(e.target.value)}
            disabled={!selectedProduct || disabled}
          />
        </div>

        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Quantity</label>
          <input
            type="number"
            className="form-control"
            min="0.001"
            step="any"
            value={quantityStr}
            onChange={(e) => setQuantityStr(e.target.value)}
            disabled={!selectedProduct || disabled}
          />
        </div>
        
        <button 
          type="button"
          className="btn btn-primary"
          onClick={handleAdd}
          disabled={!selectedProduct || disabled || parseFloat(quantityStr) <= 0}
          style={{ height: '42px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
        >
          <Plus size={16} /> Add
        </button>
      </div>
    </div>
  );
};
