import React from 'react';
import { Wrench, X } from 'lucide-react';

interface TruckOption {
  id: number;
  registrationNumber: string;
  make: string;
}

interface MaintenanceLogModalProps {
  isOpen: boolean;
  trucks: TruckOption[];
  isSubmitting: boolean;
  serverErrors?: Record<string, string>;
  formLogTruckId: string;
  setFormLogTruckId: (id: string) => void;
  formLogDate: string;
  setFormLogDate: (date: string) => void;
  formLogType: string;
  setFormLogType: (type: string) => void;
  formLogCost: string;
  setFormLogCost: (cost: string) => void;
  formLogOdometer: string;
  setFormLogOdometer: (odometer: string) => void;
  onClose: () => void;
  onSubmit: (e: React.FormEvent) => void;
}

export const MaintenanceLogModal: React.FC<MaintenanceLogModalProps> = ({
  isOpen,
  trucks,
  isSubmitting,
  serverErrors = {},
  formLogTruckId,
  setFormLogTruckId,
  formLogDate,
  setFormLogDate,
  formLogType,
  setFormLogType,
  formLogCost,
  setFormLogCost,
  formLogOdometer,
  setFormLogOdometer,
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
            <Wrench size={18} style={{ color: 'var(--primary-color)' }} /> Log Maintenance Service
          </h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={onSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Select Truck <span style={{ color: 'var(--danger)' }}>*</span></label>
              <select
                value={formLogTruckId}
                onChange={(e) => setFormLogTruckId(e.target.value)}
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

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Date <span style={{ color: 'var(--danger)' }}>*</span></label>
                <input
                  type="date"
                  value={formLogDate}
                  onChange={(e) => setFormLogDate(e.target.value)}
                  required
                  disabled={isSubmitting}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                />
                {serverErrors.date && (
                  <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.date}</span>
                )}
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Service Type</label>
                <select
                  value={formLogType}
                  onChange={(e) => setFormLogType(e.target.value)}
                  disabled={isSubmitting}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                >
                  <option value="Routine Service">Routine Service</option>
                  <option value="Repair">Repair</option>
                  <option value="Tire Replacement">Tire Replacement</option>
                  <option value="Inspection">Inspection</option>
                </select>
                {serverErrors.type && (
                  <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.type}</span>
                )}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Cost (₹) <span style={{ color: 'var(--danger)' }}>*</span></label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formLogCost}
                  onChange={(e) => setFormLogCost(e.target.value)}
                  required
                  disabled={isSubmitting}
                  placeholder="e.g. 150.00"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                />
                {serverErrors.cost && (
                  <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.cost}</span>
                )}
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>Odometer Reading (km)</label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={formLogOdometer}
                  onChange={(e) => setFormLogOdometer(e.target.value)}
                  disabled={isSubmitting}
                  placeholder="Optional"
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
                />
                {serverErrors.odometerReading && (
                  <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{serverErrors.odometerReading}</span>
                )}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem', borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem' }}>
            <button type="button" onClick={onClose} disabled={isSubmitting} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={isSubmitting || !formLogTruckId || !formLogCost} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Wrench size={16} /> {isSubmitting ? 'Logging...' : 'Save Log'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
