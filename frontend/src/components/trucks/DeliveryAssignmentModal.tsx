import React from 'react';
import { Package, X } from 'lucide-react';

interface InvoiceOption {
  id: number;
  invoiceNumber: string;
  customerName?: string;
}

interface TruckOption {
  id: number;
  registrationNumber: string;
  make: string;
}

interface DeliveryAssignmentModalProps {
  isOpen: boolean;
  trucks: TruckOption[];
  invoices: InvoiceOption[];
  isSubmitting: boolean;
  serverErrors?: Record<string, string>;
  formAssignTruckId: string;
  setFormAssignTruckId: (id: string) => void;
  formAssignInvoiceId: string;
  setFormAssignInvoiceId: (id: string) => void;
  formAssignStatus: string;
  setFormAssignStatus: (status: string) => void;
  onClose: () => void;
  onSubmit: (e: React.FormEvent) => void;
}

export const DeliveryAssignmentModal: React.FC<DeliveryAssignmentModalProps> = ({
  isOpen,
  trucks,
  invoices,
  isSubmitting,
  serverErrors = {},
  formAssignTruckId,
  setFormAssignTruckId,
  formAssignInvoiceId,
  setFormAssignInvoiceId,
  formAssignStatus,
  setFormAssignStatus,
  onClose,
  onSubmit
}) => {
  if (!isOpen) return null;

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0, 0, 0, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 1000, backdropFilter: 'blur(4px)'
    }}>
      <div className="glass-panel" style={{ width: '90%', maxWidth: '500px', background: 'var(--bg-card)', padding: '2rem', borderRadius: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Package size={18} style={{ color: 'var(--primary-color)' }} /> New Delivery Assignment
          </h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={onSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Assign to Truck <span style={{ color: 'var(--danger)' }}>*</span></label>
              <select
                value={formAssignTruckId}
                onChange={(e) => setFormAssignTruckId(e.target.value)}
                required
                disabled={isSubmitting}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              >
                <option value="">Select a Truck</option>
                {trucks.map((t) => (
                  <option key={t.id} value={t.id}>{t.registrationNumber} ({t.make})</option>
                ))}
              </select>
              {serverErrors.truckId && (
                <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.truckId}</span>
              )}
            </div>

            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Sales Invoice (Optional)</label>
              <select
                value={formAssignInvoiceId}
                onChange={(e) => setFormAssignInvoiceId(e.target.value)}
                disabled={isSubmitting}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              >
                <option value="">No specific invoice (Standard Delivery)</option>
                {invoices.map((inv) => (
                  <option key={inv.id} value={inv.id}>
                    {inv.invoiceNumber} {inv.customerName ? `- ${inv.customerName}` : ''}
                  </option>
                ))}
              </select>
              {serverErrors.invoiceId && (
                <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.invoiceId}</span>
              )}
            </div>

            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Initial Status</label>
              <select
                value={formAssignStatus}
                onChange={(e) => setFormAssignStatus(e.target.value)}
                disabled={isSubmitting}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
              >
                <option value="ASSIGNED">Pending Dispatch (Assigned)</option>
                <option value="IN_TRANSIT">Out for Delivery (In Transit)</option>
                <option value="DELIVERED">Delivered</option>
              </select>
              {serverErrors.status && (
                <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.status}</span>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem', borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem' }}>
            <button type="button" onClick={onClose} disabled={isSubmitting} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={isSubmitting || !formAssignTruckId} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Package size={16} /> {isSubmitting ? 'Assigning...' : 'Create Assignment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
