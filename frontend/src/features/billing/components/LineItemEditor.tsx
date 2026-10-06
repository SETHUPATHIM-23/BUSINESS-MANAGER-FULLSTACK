import React from 'react';
import { Trash2 } from 'lucide-react';

export interface InvoiceLineResponseDto {
  id?: number;
  productId: number;
  productSku?: string;
  productName: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  taxAmount: number;
  lineTotal: number;
}

interface LineItemEditorProps {
  lines: InvoiceLineResponseDto[];
  onRemove: (index: number) => void;
  isLoading?: boolean;
}

export const LineItemEditor: React.FC<LineItemEditorProps> = ({ lines, onRemove, isLoading = false }) => {
  return (
    <div style={{ backgroundColor: '#1e293b', borderRadius: '8px', border: '1px solid var(--border-color)', overflow: 'hidden' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
        <thead style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderBottom: '1px solid var(--border-color)' }}>
          <tr>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Product</th>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Qty</th>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Rate</th>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Discount</th>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Tax</th>
            <th style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', fontWeight: 600 }}>Total</th>
            <th style={{ padding: '0.75rem 1rem', width: '40px' }}></th>
          </tr>
        </thead>
        <tbody>
          {lines.length === 0 ? (
            <tr>
              <td colSpan={7} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                No items added yet.
              </td>
            </tr>
          ) : (
            lines.map((line, idx) => (
              <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', opacity: isLoading ? 0.6 : 1, transition: 'opacity 0.2s' }}>
                <td style={{ padding: '1rem', color: '#fff' }}>
                  <div style={{ fontWeight: 500 }}>{line.productName || 'Unknown Product'}</div>
                  {line.description && <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{line.description}</div>}
                </td>
                <td style={{ padding: '1rem', color: '#fff' }}>{line.quantity}</td>
                <td style={{ padding: '1rem', color: '#fff' }}>₹{line.unitPrice?.toFixed(2)}</td>
                <td style={{ padding: '1rem', color: 'var(--warning)' }}>
                  {line.discount > 0 ? `-₹${line.discount.toFixed(2)}` : '-'}
                </td>
                <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                  {line.taxAmount > 0 ? `₹${line.taxAmount.toFixed(2)}` : '-'}
                </td>
                <td style={{ padding: '1rem', color: '#fff', fontWeight: 600 }}>
                  ₹{line.lineTotal?.toFixed(2)}
                </td>
                <td style={{ padding: '1rem' }}>
                  <button 
                    onClick={() => onRemove(idx)}
                    style={{ background: 'none', border: 'none', color: 'var(--danger)', cursor: 'pointer', display: 'flex', padding: '4px' }}
                    title="Remove Item"
                  >
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};
