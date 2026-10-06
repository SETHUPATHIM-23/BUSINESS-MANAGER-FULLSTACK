import React from 'react';
import { DollarSign, CheckCircle } from 'lucide-react';

import type { InvoiceLineResponseDto } from './LineItemEditor';

export interface InvoiceResponseDto {
  id?: number;
  invoiceNumber?: string;
  invoiceDate?: string;
  customerName?: string;
  subtotal: number;
  taxTotal: number;
  grandTotal: number;
  cgst: number;
  sgst: number;
  igst?: number;
  roundOff: number;
  lines?: InvoiceLineResponseDto[];
}

interface InvoiceSummaryPanelProps {
  previewData: InvoiceResponseDto | null;
  paymentAmount: string;
  onPaymentAmountChange: (val: string) => void;
  onSaveDraft: () => void;
  onCompleteSale: () => void;
  isProcessing?: boolean;
  canPost?: boolean;
  hidePayment?: boolean;
}

export const InvoiceSummaryPanel: React.FC<InvoiceSummaryPanelProps> = ({
  previewData,
  paymentAmount,
  onPaymentAmountChange,
  onSaveDraft,
  onCompleteSale,
  isProcessing = false,
  canPost = true,
  hidePayment = false
}) => {
  const isReady = previewData !== null;
  const grandTotal = previewData?.grandTotal || 0;

  return (
    <div style={{ backgroundColor: '#111827', padding: '1.5rem', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      
      {/* Financial Breakdown */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', paddingBottom: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          <span>Taxable Value (Subtotal)</span>
          <span>₹{previewData?.subtotal?.toFixed(2) || '0.00'}</span>
        </div>
        
        {(previewData?.cgst ?? 0) > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            <span>CGST</span>
            <span>₹{(previewData?.cgst ?? 0).toFixed(2)}</span>
          </div>
        )}
        
        {(previewData?.sgst ?? 0) > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            <span>SGST</span>
            <span>₹{(previewData?.sgst ?? 0).toFixed(2)}</span>
          </div>
        )}

        {((previewData?.taxTotal ?? 0) > 0 && (previewData?.cgst ?? 0) === 0 && (previewData?.sgst ?? 0) === 0) && (
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            <span>Total Tax</span>
            <span>₹{(previewData?.taxTotal ?? 0).toFixed(2)}</span>
          </div>
        )}
        
        {previewData?.roundOff !== 0 && previewData?.roundOff !== undefined && (
          <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            <span>Round Off</span>
            <span>₹{previewData.roundOff.toFixed(2)}</span>
          </div>
        )}
        
        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#fff', fontSize: '1.2rem', fontWeight: 700, marginTop: '0.5rem' }}>
          <span>Grand Total</span>
          <span style={{ color: 'var(--success)' }}>₹{grandTotal.toFixed(2)}</span>
        </div>
      </div>

      {/* Payment Collection */}
      {!hidePayment && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <DollarSign size={16} style={{ color: 'var(--text-muted)' }} />
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Payment Collected (Optional)</span>
          </div>
          <input
            type="number"
            min="0"
            step="0.01"
            placeholder={`e.g. ₹${grandTotal.toFixed(2)}`}
            value={paymentAmount}
            onChange={(e) => onPaymentAmountChange(e.target.value)}
            disabled={!isReady || isProcessing}
            style={{ 
              width: '100%', padding: '12px', borderRadius: '8px', 
              border: '1px solid var(--border-color)', backgroundColor: '#1e293b', 
              color: '#fff', fontSize: '1.1rem', outline: 'none' 
            }}
          />
        </div>
      )}

      {/* Action Buttons */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {canPost ? (
          <button
            onClick={onCompleteSale}
            disabled={!isReady || isProcessing || grandTotal <= 0}
            style={{ 
              width: '100%', padding: '1rem', borderRadius: '8px', border: 'none', 
              fontSize: '1.1rem', fontWeight: 700, display: 'flex', justifyContent: 'center', 
              alignItems: 'center', gap: '0.5rem', 
              cursor: (!isReady || isProcessing || grandTotal <= 0) ? 'not-allowed' : 'pointer', 
              backgroundColor: 'var(--success)', color: '#fff', 
              opacity: (!isReady || isProcessing || grandTotal <= 0) ? 0.6 : 1, 
              transition: 'all 0.2s' 
            }}
          >
            {isProcessing ? 'Processing...' : (
              <>
                <CheckCircle size={20} />
                Complete Sale
              </>
            )}
          </button>
        ) : (
          <button
            onClick={onSaveDraft}
            disabled={!isReady || isProcessing || grandTotal <= 0}
            style={{ 
              width: '100%', padding: '1rem', borderRadius: '8px', border: 'none', 
              fontSize: '1.1rem', fontWeight: 700, display: 'flex', justifyContent: 'center', 
              alignItems: 'center', gap: '0.5rem', 
              cursor: (!isReady || isProcessing || grandTotal <= 0) ? 'not-allowed' : 'pointer', 
              backgroundColor: 'var(--accent-primary)', color: '#fff', 
              opacity: (!isReady || isProcessing || grandTotal <= 0) ? 0.6 : 1, 
              transition: 'all 0.2s' 
            }}
          >
            {isProcessing ? 'Saving...' : 'Save Draft'}
          </button>
        )}
      </div>
      
    </div>
  );
};
