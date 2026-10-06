import React, { useState, useEffect } from 'react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import { X, Plus, Trash2, Calendar, Printer } from 'lucide-react';

interface ScheduledReportModalProps {
  onClose: () => void;
}

export const ScheduledReportModal: React.FC<ScheduledReportModalProps> = ({ onClose }) => {
  const [scheduledReports, setScheduledReports] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  // New report state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [reportType, setReportType] = useState('SALES_SUMMARY');
  const [recurrenceRule, setRecurrenceRule] = useState('DAILY');
  const [printTarget, setPrintTarget] = useState('FrontDesk-HP');

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/reports/scheduled');
      setScheduledReports(res.data);
    } catch (err) {
      toastEvents.error("Failed to load scheduled reports");
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!name) return toastEvents.error("Name is required");

    try {
      await api.post('/api/reports/scheduled', {
        name,
        description,
        reportType,
        recurrenceRule,
        printTarget,
        filterConfigJson: "{}" // Default for MVP
      });
      toastEvents.success("Scheduled Report Created");
      setIsCreating(false);
      setName('');
      setDescription('');
      fetchReports();
    } catch (err) {
      toastEvents.error("Failed to create scheduled report");
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this scheduled report?")) return;
    try {
      await api.delete(`/api/reports/scheduled/${id}`);
      toastEvents.success("Deleted successfully");
      fetchReports();
    } catch (err) {
      toastEvents.error("Failed to delete scheduled report");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="glass-panel w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-fade-in shadow-2xl">
        
        {/* Header */}
        <div className="p-5 border-b border-white/10 flex justify-between items-center bg-gradient-to-r from-blue-900/40 to-transparent">
          <div>
            <h2 className="text-xl font-bold flex items-center gap-2">
              <Calendar size={20} className="text-blue-400" />
              Scheduled Reports & Print Routing
            </h2>
            <p className="text-sm text-secondary mt-1">Configure automated report generation and direct-to-printer routing.</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-full transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-auto p-6 bg-black/40">
          
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-lg">Active Schedules</h3>
            <button 
              onClick={() => setIsCreating(!isCreating)}
              className="btn-primary text-sm h-9 px-4 flex items-center gap-2"
            >
              {isCreating ? <X size={16} /> : <Plus size={16} />} 
              {isCreating ? 'Cancel' : 'New Schedule'}
            </button>
          </div>

          {/* Creation Form */}
          {isCreating && (
            <div className="bg-white/5 border border-white/10 rounded-lg p-5 mb-6 animate-fade-in">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div className="form-group">
                  <label>Schedule Name</label>
                  <input type="text" className="form-input" value={name} onChange={e => setName(e.target.value)} placeholder="e.g., Daily Evening Sales" />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <input type="text" className="form-input" value={description} onChange={e => setDescription(e.target.value)} placeholder="Optional description" />
                </div>
                <div className="form-group">
                  <label>Report Type</label>
                  <select className="form-input" value={reportType} onChange={e => setReportType(e.target.value)}>
                    <option value="SALES_SUMMARY">Sales Summary</option>
                    <option value="INVENTORY_VALUATION">Inventory Valuation</option>
                    <option value="FINANCIAL_BALANCE">Financial Balance (P&L)</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Recurrence</label>
                  <select className="form-input" value={recurrenceRule} onChange={e => setRecurrenceRule(e.target.value)}>
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </div>
                <div className="form-group md:col-span-2">
                  <label>Print Target</label>
                  <div className="flex gap-2">
                    <div className="bg-white/10 px-3 flex items-center justify-center rounded-l-md border border-r-0 border-white/20">
                      <Printer size={16} className="text-secondary" />
                    </div>
                    <input type="text" className="form-input rounded-l-none" value={printTarget} onChange={e => setPrintTarget(e.target.value)} placeholder="e.g., FrontDesk-HP, Warehouse-Laser" />
                  </div>
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setIsCreating(false)} className="btn-secondary h-9 px-4">Cancel</button>
                <button onClick={handleCreate} className="btn-primary h-9 px-6">Save Schedule</button>
              </div>
            </div>
          )}

          {/* List */}
          {loading ? (
            <div className="flex justify-center p-8 text-secondary">Loading schedules...</div>
          ) : scheduledReports.length === 0 ? (
            <div className="text-center p-8 border border-dashed border-white/20 rounded-lg text-secondary">
              No scheduled reports configured. Click 'New Schedule' to automate a report.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {scheduledReports.map(sr => (
                <div key={sr.id} className="bg-white/5 border border-white/10 rounded-lg p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-1">
                      <h4 className="font-bold text-lg">{sr.name}</h4>
                      <span className="text-xs px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                        {sr.reportType}
                      </span>
                    </div>
                    <p className="text-sm text-secondary mb-2">{sr.description}</p>
                    <div className="flex flex-wrap gap-4 text-xs font-mono text-secondary">
                      <span className="flex items-center gap-1"><Calendar size={14} /> {sr.recurrenceRule}</span>
                      <span className="flex items-center gap-1"><Printer size={14} /> {sr.printTarget || 'No Printer'}</span>
                      <span className="flex items-center gap-1">Status: <strong className={sr.lastRunStatus === 'SUCCESS' ? 'text-emerald-400' : 'text-white'}>{sr.lastRunStatus || 'PENDING'}</strong></span>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <span className="text-xs text-secondary">Next Run: {sr.nextRunTime ? new Date(sr.nextRunTime).toLocaleString() : 'Pending'}</span>
                    <button 
                      onClick={() => handleDelete(sr.id)}
                      className="p-2 bg-red-500/10 text-red-400 hover:bg-red-500/20 rounded transition-colors"
                      title="Delete Schedule"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
