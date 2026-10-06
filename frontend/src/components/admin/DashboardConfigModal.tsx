import React, { useState, useEffect } from 'react';
import { X, Save, LayoutDashboard } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import type { RoleDto } from './RoleModal';

interface DashboardConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
  role: RoleDto | null;
}

const AVAILABLE_WIDGETS = [
  { code: 'KPI_SALES', label: 'Sales KPI (Today & Month)', desc: 'Total sales revenue, invoice counts, and growth metrics.' },
  { code: 'KPI_PURCHASES', label: 'Purchases KPI (Today & Month)', desc: 'Vendor orders, purchase expenses, and procurement stats.' },
  { code: 'KPI_FUNDS', label: 'Liquidity KPI (Cash & Bank)', desc: 'Real-time account balances across cash and bank funds.' },
  { code: 'KPI_AGING', label: 'Net Position KPI (AR vs AP)', desc: 'Receivables vs Payables net working capital indicator.' },
  { code: 'ALERT_STOCK', label: 'Low Stock Alerts Panel', desc: 'Alert cards for items below reorder threshold.' },
  { code: 'ACTIVITY_FEED', label: 'Recent Activity Feed', desc: 'Live audit log feed of system actions.' },
  { code: 'SYSTEM_STATUS', label: 'System Status (Backups & Health)', desc: 'Automated backup health and database status.' },
  { code: 'KPI_AGING_DETAIL', label: 'Receivables Aging Detail Table', desc: 'Customer balance breakdown by 30/60/90 days.' },
];

export const DashboardConfigModal: React.FC<DashboardConfigModalProps> = ({ isOpen, onClose, role }) => {
  const [enabledWidgets, setEnabledWidgets] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen && role) {
      fetchConfig();
    }
  }, [isOpen, role]);

  const fetchConfig = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/api/admins/roles/${role?.id}/dashboard-config`);
      setEnabledWidgets(res.data || []);
    } catch (err) {
      toastEvents.error('Failed to load dashboard configuration for this role');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (code: string) => {
    setEnabledWidgets(prev => 
      prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!role) return;

    setSaving(true);
    try {
      await api.put(`/api/admins/roles/${role.id}/dashboard-config`, enabledWidgets);
      toastEvents.success(`Dashboard widgets configured for ${role.name}`);
      onClose();
    } catch (err) {
      toastEvents.error('Failed to save dashboard configuration');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen || !role) return null;

  return (
    <div className="modal-backdrop" style={{ zIndex: 1000 }}>
      <div className="glass-panel" style={{ width: '560px', maxWidth: '95vw', padding: '1.75rem', borderRadius: 'var(--border-radius-lg)', boxShadow: 'var(--shadow-lg)' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ width: '38px', height: '38px', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(59, 130, 246, 0.12)', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <LayoutDashboard size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                Dashboard Config: {role.name}
              </h2>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
                Select widgets enabled on the executive dashboard for this role.
              </p>
            </div>
          </div>
          <button onClick={onClose} className="btn btn-outline btn-sm" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {loading ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              Loading widgets…
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', maxHeight: '340px', overflowY: 'auto', paddingRight: '4px' }}>
              {AVAILABLE_WIDGETS.map(widget => {
                const isEnabled = enabledWidgets.includes(widget.code);
                return (
                  <div
                    key={widget.code}
                    onClick={() => handleToggle(widget.code)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.75rem 1rem',
                      borderRadius: 'var(--border-radius-sm)',
                      backgroundColor: isEnabled ? 'rgba(37, 99, 235, 0.08)' : 'var(--bg-tertiary)',
                      border: `1px solid ${isEnabled ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                      cursor: 'pointer',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <div style={{ flex: 1, paddingRight: '1rem' }}>
                      <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', display: 'block' }}>
                        {widget.label}
                      </span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '2px' }}>
                        {widget.desc}
                      </span>
                    </div>

                    {/* Toggle switch visual */}
                    <div style={{
                      width: '40px',
                      height: '22px',
                      borderRadius: '12px',
                      backgroundColor: isEnabled ? 'var(--accent-primary)' : 'var(--border-color)',
                      position: 'relative',
                      transition: 'background-color 0.2s ease',
                      flexShrink: 0
                    }}>
                      <div style={{
                        width: '18px',
                        height: '18px',
                        borderRadius: '50%',
                        backgroundColor: '#ffffff',
                        position: 'absolute',
                        top: '2px',
                        left: isEnabled ? '20px' : '2px',
                        transition: 'left 0.2s ease',
                        boxShadow: 'var(--shadow-sm)'
                      }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Action Buttons */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', marginTop: '1.25rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving || loading}>
              <Save size={16} /> {saving ? 'Saving…' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
