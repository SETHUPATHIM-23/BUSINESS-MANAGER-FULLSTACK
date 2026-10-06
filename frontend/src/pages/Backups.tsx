import React, { useState, useEffect } from 'react';
import { api } from '../utils/api';
import { toastEvents } from '../utils/toast';
import { RestoreModal } from '../components/admin/RestoreModal';
import {
  Database, RefreshCw, Plus, CheckCircle,
  HardDrive, FolderOpen, RotateCcw, Clock, AlertTriangle, Edit3, Save, X
} from 'lucide-react';

interface BackupJobDto {
  id: number;
  startTime: string;
  endTime: string;
  result: string;
  archiveLocation: string;
  fileSizeBytes: number;
  verificationOutcome: string;
  initiatedBy: string;
  createdAt: string;
}

export const Backups: React.FC = () => {
  const [jobs, setJobs] = useState<BackupJobDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [backupLocation, setBackupLocation] = useState<string>('');
  
  // Edit Backup Location state
  const [isEditingLocation, setIsEditingLocation] = useState(false);
  const [newLocationInput, setNewLocationInput] = useState('');
  const [savingLocation, setSavingLocation] = useState(false);

  // Restore modal state
  const [selectedBackupForRestore, setSelectedBackupForRestore] = useState<BackupJobDto | null>(null);
  const [isRestoreModalOpen, setIsRestoreModalOpen] = useState(false);
  const [restoring, setRestoring] = useState(false);

  const fetchBackups = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/v1/backups?size=10&sort=createdAt,desc');
      const data = response.data.content || response.data || [];
      const active = data.filter((j: BackupJobDto) => j.result !== 'PURGED').slice(0, 5);
      setJobs(active);
    } catch {
      toastEvents.error('Failed to fetch backup list');
    } finally {
      setLoading(false);
    }
  };

  const fetchLocation = async () => {
    try {
      const res = await api.get('/api/v1/backups/location');
      const loc = res.data || '';
      setBackupLocation(loc);
      setNewLocationInput(loc);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    fetchBackups();
    fetchLocation();
  }, []);

  const handleSaveLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newLocationInput.trim()) {
      toastEvents.error('Backup location path cannot be empty');
      return;
    }
    try {
      setSavingLocation(true);
      const res = await api.post('/api/v1/backups/location', { path: newLocationInput.trim() });
      setBackupLocation(res.data);
      setIsEditingLocation(false);
      toastEvents.success('Backup folder location updated');
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to update backup location');
    } finally {
      setSavingLocation(false);
    }
  };

  const handleCreateBackup = async () => {
    try {
      setTriggering(true);
      await api.post('/api/v1/backups/trigger');
      toastEvents.success('Backup started in background — will appear shortly');
      setTimeout(fetchBackups, 3500);
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to trigger backup');
    } finally {
      setTriggering(false);
    }
  };

  const handleConfirmRestore = async () => {
    if (!selectedBackupForRestore) return;
    try {
      setRestoring(true);
      await api.post(`/api/v1/backups/${selectedBackupForRestore.id}/restore`);
      toastEvents.success('Database restored successfully! Reloading system...');
      setIsRestoreModalOpen(false);
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to restore database backup');
      setRestoring(false);
    }
  };

  const getFileName = (path: string | null) => {
    if (!path) return 'backup_file.zip';
    return path.split(/[/\\]/).pop() || path;
  };

  const formatBytes = (bytes: number) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dt: string) => {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  const latestBackup = jobs[0];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon"><Database size={22} /></div>
          <div>
            <h1 className="page-title">Database Backups & Disaster Recovery</h1>
            <p className="page-description">
              Automated database snapshots every 2 days. Retention limit: 5 rolling backups.
            </p>
          </div>
        </div>
        <div className="header-actions">
          <button onClick={fetchBackups} className="btn btn-secondary" title="Refresh list">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
          </button>
          <button onClick={handleCreateBackup} disabled={triggering} className="btn btn-primary">
            <Plus size={16} /> {triggering ? 'Creating Snapshot…' : 'Backup Now'}
          </button>
        </div>
      </div>

      {/* Backup Folder Location Configuration Banner */}
      <div className="glass-panel mb-4" style={{ padding: '1.25rem 1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flex: 1, minWidth: '280px' }}>
            <div style={{ padding: '0.6rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-primary)' }}>
              <FolderOpen size={20} />
            </div>
            <div style={{ flex: 1 }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Storage Folder Location
              </span>
              {!isEditingLocation ? (
                <div style={{ fontFamily: 'monospace', fontSize: '0.95rem', fontWeight: 600, marginTop: '2px', color: 'var(--text-primary)' }}>
                  {backupLocation || 'Loading path…'}
                </div>
              ) : (
                <form onSubmit={handleSaveLocation} style={{ display: 'flex', gap: '0.5rem', marginTop: '4px', maxWidth: '500px' }}>
                  <input
                    type="text"
                    className="form-input"
                    style={{ fontSize: '0.85rem', fontFamily: 'monospace', padding: '0.4rem 0.75rem' }}
                    value={newLocationInput}
                    onChange={(e) => setNewLocationInput(e.target.value)}
                    placeholder="e.g. C:\Backups\SMCManagement"
                    autoFocus
                  />
                  <button type="submit" className="btn btn-primary btn-sm" disabled={savingLocation}>
                    <Save size={14} /> {savingLocation ? 'Saving…' : 'Save'}
                  </button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => setIsEditingLocation(false)}>
                    <X size={14} />
                  </button>
                </form>
              )}
            </div>
          </div>

          {!isEditingLocation && (
            <button onClick={() => setIsEditingLocation(true)} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
              <Edit3 size={14} /> Change Location
            </button>
          )}
        </div>
      </div>

      {/* KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'var(--success-bg)', color: 'var(--success-text)' }}>
            <CheckCircle size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Available Snapshots</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>
              {jobs.filter(j => j.result === 'SUCCESS').length} / 5 Max
            </h3>
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'var(--info-bg)', color: 'var(--info-text)' }}>
            <Clock size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Last Backup</span>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 600, marginTop: '4px' }}>
              {latestBackup ? formatDate(latestBackup.createdAt || latestBackup.startTime) : 'No backups'}
            </h3>
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'var(--bg-tertiary)', color: 'var(--accent-primary)' }}>
            <HardDrive size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Total Disk Used</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>
              {formatBytes(jobs.reduce((acc, j) => acc + (j.fileSizeBytes || 0), 0))}
            </h3>
          </div>
        </div>
      </div>

      {/* Backup list cards */}
      <h2 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Database size={18} style={{ color: 'var(--accent-primary)' }} />
        Recent Database Snapshots
      </h2>

      {loading && jobs.length === 0 ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading backups…
        </div>
      ) : jobs.length === 0 ? (
        <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Database size={44} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
          <p style={{ fontSize: '1rem', fontWeight: 500 }}>No database backups created yet.</p>
          <p style={{ fontSize: '0.85rem', marginTop: '4px' }}>Click <strong>Backup Now</strong> to manually create your first snapshot.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {jobs.map((job, index) => {
            const isSuccess = job.result === 'SUCCESS';
            const isInProgress = job.result === 'IN_PROGRESS';
            const fileName = getFileName(job.archiveLocation);
            return (
              <div
                key={job.id}
                className="glass-panel"
                style={{
                  padding: '1.25rem 1.5rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  borderLeft: `4px solid ${isSuccess ? 'var(--success-text)' : isInProgress ? 'var(--accent-primary)' : 'var(--danger)'}`,
                }}
              >
                {/* Status indicator */}
                <div style={{ flexShrink: 0 }}>
                  {isSuccess ? (
                    <CheckCircle size={26} style={{ color: 'var(--success-text)' }} />
                  ) : isInProgress ? (
                    <RefreshCw size={26} className="animate-spin" style={{ color: 'var(--accent-primary)' }} />
                  ) : (
                    <AlertTriangle size={26} style={{ color: 'var(--danger)' }} />
                  )}
                </div>

                {/* Info block */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                    <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.92rem', color: 'var(--text-primary)' }}>
                      {fileName}
                    </span>
                    {index === 0 && (
                      <span className="badge badge-success" style={{ fontSize: '0.65rem', padding: '2px 8px' }}>LATEST SNAPSHOT</span>
                    )}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px', display: 'flex', gap: '1.25rem', flexWrap: 'wrap' }}>
                    <span>📅 {formatDate(job.createdAt || job.startTime)}</span>
                    <span>💾 {formatBytes(job.fileSizeBytes)}</span>
                    <span>👤 {job.initiatedBy || 'SYSTEM'}</span>
                  </div>
                </div>

                {/* Rollback button */}
                {isSuccess && (
                  <button
                    onClick={() => {
                      setSelectedBackupForRestore(job);
                      setIsRestoreModalOpen(true);
                    }}
                    className="btn btn-danger btn-sm"
                    style={{
                      flexShrink: 0,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '0.45rem 0.9rem',
                      fontWeight: 600
                    }}
                    title="Rollback database to this backup"
                  >
                    <RotateCcw size={15} /> Restore / Rollback
                  </button>
                )}
                {isInProgress && (
                  <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--accent-primary)', flexShrink: 0 }}>
                    In progress…
                  </span>
                )}
                {!isSuccess && !isInProgress && (
                  <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--danger)', flexShrink: 0 }}>
                    Snapshot Failed (No File)
                  </span>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Safety Confirmation Modal */}
      <RestoreModal
        isOpen={isRestoreModalOpen}
        onClose={() => {
          setIsRestoreModalOpen(false);
          setSelectedBackupForRestore(null);
        }}
        onConfirm={handleConfirmRestore}
        backup={selectedBackupForRestore}
        restoring={restoring}
      />
    </div>
  );
};
