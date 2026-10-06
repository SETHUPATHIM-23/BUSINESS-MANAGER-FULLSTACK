import { useState, useEffect } from 'react';
import { api } from '../utils/api';
import { 
  TrendingUp, TrendingDown, DollarSign, CreditCard, 
  Package, Activity, ShieldAlert, Clock,
  FileText, ShoppingCart, Users
} from 'lucide-react';
import { toastEvents } from '../utils/toast';
import { useNavigate } from 'react-router-dom';

export interface CustomerAgingDetail {
  customerId?: number;
  customerName: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  outstandingAmount: number;
  ageInDays: number;
  statusCategory: string;
  currentAmount: number;
  days30Amount: number;
  days60Amount: number;
  days90PlusAmount: number;
}

interface AgingBucket {
  current: number;
  days30: number;
  days60: number;
  days90Plus: number;
  total: number;
  details?: CustomerAgingDetail[];
}

interface DashboardMetrics {
  salesToday: number;
  salesThisMonth: number;
  purchasesToday: number;
  purchasesThisMonth: number;
  cashBalance: number;
  bankBalance: number;
  receivablesAging: AgingBucket;
  payablesAging: AgingBucket;
}

interface ProductAlert {
  id: number;
  sku: string;
  name: string;
  stockOnHand: number;
  reorderLevel: number;
}

interface AuditLogEntry {
  id: number;
  username: string;
  actionType: string;
  moduleName: string;
  createdAt: string;
}

interface DashboardAlerts {
  lowStockAlerts: ProductAlert[];
  mostRecentBackupStatus: string;

  recentActivities: AuditLogEntry[];
}

interface DashboardResponse {
  enabledWidgets: string[];
  metrics: DashboardMetrics;
  alerts: DashboardAlerts;
}

const formatCurrency = (val: number) => 
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val || 0);

export const Dashboard = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);


  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await api.get('/api/dashboard');
        setData(response.data);
        setError(null);
        

      } catch (err: any) {
        setError('Failed to load dashboard data. Check your network or permissions.');
        toastEvents.error('Dashboard error', err.response?.data?.message || err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
    // Poll every 30 seconds to match cache
    const interval = setInterval(fetchDashboard, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !data) {
    return (
      <div className="page-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '50vh' }}>
        <div style={{ color: 'var(--text-secondary)', fontSize: '1.125rem' }}>Loading Dashboard Data...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '50vh' }}>
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--danger)' }}>
          <ShieldAlert size={48} style={{ margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Access Denied / Error</h2>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  if (!data) return null;

  const { metrics, alerts, enabledWidgets } = data;

  const hasWidget = (code: string) => enabledWidgets.includes(code);

  return (
    <div className="page-container dashboard-bg">
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Activity size={24} />
          </div>
          <div>
            <h1 className="page-title">Dashboard Overview</h1>
            <p className="page-description">Real-time financial and operational aggregates</p>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
        
        {/* KPI: Sales */}
        {hasWidget('KPI_SALES') && (
          <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Sales Today</p>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success)', margin: '0.25rem 0' }}>{formatCurrency(metrics.salesToday)}</h3>
              </div>
              <div style={{ padding: '0.75rem', borderRadius: '50%', background: 'rgba(16, 185, 129, 0.15)', color: 'var(--success)' }}>
                <TrendingUp size={24} />
              </div>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem' }}>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{formatCurrency(metrics.salesThisMonth)}</span> this month
            </div>
          </div>
        )}

        {/* KPI: Purchases */}
        {hasWidget('KPI_PURCHASES') && (
          <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Purchases Today</p>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--warning)', margin: '0.25rem 0' }}>{formatCurrency(metrics.purchasesToday)}</h3>
              </div>
              <div style={{ padding: '0.75rem', borderRadius: '50%', background: 'rgba(245, 158, 11, 0.15)', color: 'var(--warning)' }}>
                <TrendingDown size={24} />
              </div>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem' }}>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{formatCurrency(metrics.purchasesThisMonth)}</span> this month
            </div>
          </div>
        )}

        {/* KPI: Funds */}
        {hasWidget('KPI_FUNDS') && (
          <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total Liquidity</p>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--info)', margin: '0.25rem 0' }}>
                  {formatCurrency(metrics.cashBalance + metrics.bankBalance)}
                </h3>
              </div>
              <div style={{ padding: '0.75rem', borderRadius: '50%', background: 'rgba(59, 130, 246, 0.15)', color: 'var(--info)' }}>
                <DollarSign size={24} />
              </div>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between' }}>
              <span>Cash: <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{formatCurrency(metrics.cashBalance)}</span></span>
              <span>Bank: <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{formatCurrency(metrics.bankBalance)}</span></span>
            </div>
          </div>
        )}

        {/* KPI: Aging summary */}
        {hasWidget('KPI_AGING') && (
          <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Net Position</p>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#c084fc', margin: '0.25rem 0' }}>
                  {formatCurrency(metrics.receivablesAging.total - metrics.payablesAging.total)}
                </h3>
              </div>
              <div style={{ padding: '0.75rem', borderRadius: '50%', background: 'rgba(192, 132, 252, 0.15)', color: '#c084fc' }}>
                <CreditCard size={24} />
              </div>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between' }}>
              <span>AR: <span style={{ color: 'var(--success)', fontWeight: 600 }}>{formatCurrency(metrics.receivablesAging.total)}</span></span>
              <span>AP: <span style={{ color: 'var(--warning)', fontWeight: 600 }}>{formatCurrency(metrics.payablesAging.total)}</span></span>
            </div>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="header-actions" style={{ justifyContent: 'flex-start', flexWrap: 'wrap', marginBottom: '2rem' }}>
        <button className="btn btn-primary" onClick={() => navigate('/billing?action=new')}>
          <FileText size={16} /> New Invoice
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/purchases?action=new')}>
          <ShoppingCart size={16} /> New Purchase
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/funds?action=new')}>
          <DollarSign size={16} /> New Payment
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/customers?action=new')}>
          <Users size={16} /> New Customer
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/products?action=new')}>
          <Package size={16} /> New Product
        </button>
      </div>



      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 340px), 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>


        {hasWidget('ACTIVITY_FEED') && (
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <Activity style={{ color: 'var(--info)' }} size={24} />
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>Recent Activity</h2>
            </div>
            {alerts.recentActivities.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No recent activity.</p>
            ) : (
              <ul style={{ display: 'flex', flexDirection: 'column', gap: '1rem', padding: 0, margin: 0, listStyle: 'none' }}>
                {alerts.recentActivities.map(log => (
                  <li key={log.id} style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start', padding: '0.75rem', borderRadius: '8px', backgroundColor: 'var(--bg-tertiary)' }}>
                    <div style={{ padding: '0.5rem', background: 'rgba(59, 130, 246, 0.15)', color: 'var(--info)', borderRadius: '6px' }}>
                      <Clock size={16} />
                    </div>
                    <div>
                      <p style={{ fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 0.25rem 0' }}>{log.actionType.replace(/_/g, ' ')}</p>
                      <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: '0 0 0.25rem 0' }}>
                        <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{log.username}</span> in {log.moduleName}
                      </p>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: 0 }}>
                        {new Date(log.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      {(hasWidget('SYSTEM_STATUS') || hasWidget('KPI_AGING_DETAIL')) && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 340px), 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
          {hasWidget('SYSTEM_STATUS') && (
             <div className="glass-panel" style={{ padding: '1.5rem' }}>
               <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 1.5rem 0', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                 <ShieldAlert style={{ color: 'var(--text-muted)' }} size={24} /> System Status
               </h2>
               <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                 <div style={{ padding: '1rem', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: 'var(--bg-tertiary)' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>Backups</span>
                    <span style={{ fontWeight: 600, color: alerts.mostRecentBackupStatus.includes('SUCCESS') ? 'var(--success)' : alerts.mostRecentBackupStatus.includes('FAILED') ? 'var(--danger)' : alerts.mostRecentBackupStatus.includes('WARNING') ? 'var(--warning)' : 'var(--text-muted)' }}>
                      {alerts.mostRecentBackupStatus}
                    </span>
                 </div>

               </div>
            </div>
          )}

          {hasWidget('KPI_AGING_DETAIL') && (
            <div className="glass-panel" style={{ padding: '1.5rem', gridColumn: '1 / -1' }}>
               <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                  <div>
                    <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>Receivables Aging Detail</h2>
                    <p style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', margin: '0.2rem 0 0 0' }}>Customer account balances and age of receivables</p>
                  </div>
                  <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Total Receivables: <strong style={{ color: 'var(--accent-primary)', fontSize: '0.95rem' }}>{formatCurrency(metrics.receivablesAging.total)}</strong>
                    </span>
                  </div>
               </div>

               <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr className="table-header-row">
                        <th>Customer Name</th>
                        <th>Invoice No</th>
                        <th>Invoice Date</th>
                        <th>Due Date</th>
                        <th style={{ textAlign: 'center' }}>Age of Receivable</th>
                        <th style={{ textAlign: 'right' }}>Outstanding Amount</th>
                        <th style={{ textAlign: 'center' }}>Aging Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {metrics.receivablesAging.details && metrics.receivablesAging.details.length > 0 ? (
                        metrics.receivablesAging.details.map((d, i) => (
                          <tr key={i} className="table-row">
                            <td style={{ fontWeight: 600 }}>{d.customerName}</td>
                            <td style={{ fontFamily: 'monospace', fontWeight: 500 }}>{d.invoiceNumber}</td>
                            <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                              {d.invoiceDate ? new Date(d.invoiceDate).toLocaleDateString('en-IN') : '-'}
                            </td>
                            <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                              {d.dueDate ? new Date(d.dueDate).toLocaleDateString('en-IN') : '-'}
                            </td>
                            <td style={{ textAlign: 'center', fontWeight: 700, color: 'var(--accent-primary)' }}>
                              {d.ageInDays} {d.ageInDays === 1 ? 'Day' : 'Days'} Old
                            </td>
                            <td style={{ textAlign: 'right', fontWeight: 700, color: 'var(--text-primary)' }}>
                              {formatCurrency(d.outstandingAmount)}
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <span className={
                                d.statusCategory === 'Current' ? 'badge badge-success' :
                                d.statusCategory === '1-30 Days' ? 'badge badge-warning' :
                                'badge badge-danger'
                              }>
                                {d.statusCategory}
                              </span>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr className="table-row">
                          <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2.5rem' }}>
                            No outstanding receivables found across customer accounts.
                          </td>
                        </tr>
                      )}
                    </tbody>
                    {metrics.receivablesAging.details && metrics.receivablesAging.details.length > 0 && (
                      <tfoot>
                        <tr style={{ backgroundColor: 'var(--bg-tertiary)', fontWeight: 700, borderTop: '2px solid var(--border-color)' }}>
                          <td colSpan={5} style={{ padding: '12px 16px', fontSize: '0.85rem', textTransform: 'uppercase' }}>
                            Total Summary
                          </td>
                          <td style={{ textAlign: 'right', padding: '12px 16px', color: 'var(--accent-primary)', fontSize: '0.95rem' }}>
                            {formatCurrency(metrics.receivablesAging.total)}
                          </td>
                          <td style={{ textAlign: 'center', padding: '12px 16px', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            Current: {formatCurrency(metrics.receivablesAging.current)} | 30d+: {formatCurrency(metrics.receivablesAging.days30 + metrics.receivablesAging.days60 + metrics.receivablesAging.days90Plus)}
                          </td>
                        </tr>
                      </tfoot>
                    )}
                  </table>
               </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
