import React from 'react';
import { Clock, Trash2 } from 'lucide-react';

export interface DeliveryAssignmentDto {
  id: number;
  truckId: number;
  truckRegistrationNumber: string;
  invoiceId: number | null;
  invoiceNumber: string | null;
  customerName: string | null;
  status: 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
}

interface DeliveryKanbanBoardProps {
  assignments: DeliveryAssignmentDto[];
  loading: boolean;
  viewMode: 'kanban' | 'table';
  hasWriteAccess: boolean;
  onUpdateStatus: (id: number, status: string) => void;
  onDelete: (id: number) => void;
}

export const DeliveryKanbanBoard: React.FC<DeliveryKanbanBoardProps> = ({
  assignments,
  loading,
  viewMode,
  hasWriteAccess,
  onUpdateStatus,
  onDelete
}) => {

  if (viewMode === 'kanban') {
    return (
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem', alignItems: 'start' }}>
        {[
          { key: 'ASSIGNED', title: 'Pending Dispatch', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.1)', border: 'rgba(245, 158, 11, 0.3)' },
          { key: 'IN_TRANSIT', title: 'Out for Delivery', color: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)', border: 'rgba(59, 130, 246, 0.3)' },
          { key: 'DELIVERED', title: 'Delivered', color: '#10b981', bg: 'rgba(16, 185, 129, 0.1)', border: 'rgba(16, 185, 129, 0.3)' },
          { key: 'CANCELLED', title: 'Cancelled', color: '#ef4444', bg: 'rgba(239, 68, 68, 0.1)', border: 'rgba(239, 68, 68, 0.3)' }
        ].map(column => {
          const columnAssignments = assignments.filter(a => a.status === column.key);
          return (
            <div
              key={column.key}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                const idStr = e.dataTransfer.getData('assignmentId');
                if (idStr && hasWriteAccess) {
                  onUpdateStatus(parseInt(idStr), column.key);
                }
              }}
              className="glass-panel"
              style={{
                padding: '1.25rem', borderRadius: '18px', background: 'var(--bg-card)',
                borderTop: `4px solid ${column.color}`, minHeight: '400px'
              }}
            >
              {/* COLUMN HEADER */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {column.title}
                </h3>
                <span style={{
                  padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 700,
                  background: column.bg, color: column.color, border: `1px solid ${column.border}`
                }}>
                  {columnAssignments.length}
                </span>
              </div>

              {/* CARD LIST */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {loading ? (
                  <div style={{ padding: '2rem 1rem', textAlign: 'center' }}>
                    <div className="spinner" style={{ margin: '0 auto', width: '24px', height: '24px', border: '3px solid rgba(255,255,255,0.1)', borderTopColor: 'var(--primary-color)', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
                  </div>
                ) : columnAssignments.length === 0 ? (
                  <div style={{ padding: '2rem 1rem', textAlign: 'center', border: '2px dashed var(--border-color)', borderRadius: '12px' }}>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>Drag orders here</p>
                  </div>
                ) : (
                  columnAssignments.map(asg => (
                    <div
                      key={asg.id}
                      draggable={hasWriteAccess}
                      onDragStart={(e) => e.dataTransfer.setData('assignmentId', asg.id.toString())}
                      className="glass-panel hover-lift"
                      style={{
                        padding: '1rem', borderRadius: '14px', background: 'rgba(255, 255, 255, 0.03)',
                        border: '1px solid var(--border-color)', cursor: hasWriteAccess ? 'grab' : 'default',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                        <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--primary-color)' }}>
                          {asg.truckRegistrationNumber}
                        </span>
                        {asg.invoiceNumber && (
                          <span style={{ fontSize: '0.75rem', background: 'rgba(255,255,255,0.08)', padding: '2px 6px', borderRadius: '6px', color: 'var(--text-secondary)' }}>
                            {asg.invoiceNumber}
                          </span>
                        )}
                      </div>

                      <p style={{ fontSize: '0.85rem', color: 'var(--text-primary)', fontWeight: 500, margin: '0 0 0.5rem 0' }}>
                        {asg.customerName || 'Standard Delivery Order'}
                      </p>

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem', paddingTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={11} /> {new Date(asg.createdAt).toLocaleDateString()}
                        </span>

                        {/* QUICK ADVANCE ACTION BUTTONS */}
                        {hasWriteAccess && (
                          <div style={{ display: 'flex', gap: '4px' }}>
                            {asg.status === 'ASSIGNED' && (
                              <button
                                onClick={() => onUpdateStatus(asg.id, 'IN_TRANSIT')}
                                className="btn btn-secondary"
                                style={{ padding: '3px 8px', fontSize: '0.7rem', color: '#3b82f6', borderColor: 'rgba(59, 130, 246, 0.3)' }}
                              >
                                Dispatch &rarr;
                              </button>
                            )}
                            {asg.status === 'IN_TRANSIT' && (
                              <button
                                onClick={() => onUpdateStatus(asg.id, 'DELIVERED')}
                                className="btn btn-secondary"
                                style={{ padding: '3px 8px', fontSize: '0.7rem', color: '#10b981', borderColor: 'rgba(16, 185, 129, 0.3)' }}
                              >
                                Delivered &rarr;
                              </button>
                            )}
                            <button
                              onClick={() => onDelete(asg.id)}
                              style={{ background: 'none', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '2px' }}
                              title="Delete"
                            >
                              <Trash2 size={12} />
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  // TABLE VIEW
  return (
    <div className="glass-panel" style={{ padding: '1.5rem', borderRadius: '16px' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
            <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Truck Reg</th>
            <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Sales Invoice</th>
            <th style={{ padding: '12px', color: 'var(--text-secondary)' }}>Customer</th>
            <th style={{ padding: '12px', color: 'var(--text-secondary)', textAlign: 'center' }}>Status</th>
            <th style={{ padding: '12px', color: 'var(--text-secondary)', textAlign: 'center' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={5} style={{ padding: '20px', textAlign: 'center' }}>Loading assignments...</td></tr>
          ) : assignments.length === 0 ? (
            <tr><td colSpan={5} style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>No delivery assignments found</td></tr>
          ) : (
            assignments.map((asg) => (
              <tr key={asg.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                <td style={{ padding: '12px', fontWeight: 600, color: 'var(--primary-color)' }}>{asg.truckRegistrationNumber}</td>
                <td style={{ padding: '12px' }}>{asg.invoiceNumber || 'N/A'}</td>
                <td style={{ padding: '12px' }}>{asg.customerName || 'N/A'}</td>
                <td style={{ padding: '12px', textAlign: 'center' }}>
                  <select
                    value={asg.status}
                    onChange={(e) => onUpdateStatus(asg.id, e.target.value)}
                    disabled={!hasWriteAccess}
                    style={{ padding: '4px 8px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.8rem' }}
                  >
                    <option value="ASSIGNED">ASSIGNED</option>
                    <option value="IN_TRANSIT">IN_TRANSIT</option>
                    <option value="DELIVERED">DELIVERED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </td>
                <td style={{ padding: '12px', textAlign: 'center' }}>
                  {hasWriteAccess && (
                    <button onClick={() => onDelete(asg.id)} className="btn btn-secondary" style={{ padding: '4px 8px', color: 'var(--danger)' }}>
                      <Trash2 size={13} />
                    </button>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};
