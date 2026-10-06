import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { UserModal } from '../components/admin/UserModal';
import type { UserDto } from '../components/admin/UserModal';
import { RoleModal } from '../components/admin/RoleModal';
import type { RoleDto } from '../components/admin/RoleModal';
import { DashboardConfigModal } from '../components/admin/DashboardConfigModal';
import CompanySettings, { getStoredCompanyDetails, fetchCompanySettingsFromBackend, type CompanyDetails } from '../components/billing/CompanySettings';
import {
  Users, Shield, Activity, Search, Plus, ChevronLeft, ChevronRight, Edit, Trash2, Key, LayoutDashboard,
  Building2, Percent, RefreshCw, Eye, FileJson, CheckCircle2, X, Palette
} from 'lucide-react';
import { useTheme, THEME_COLORS, type ThemeColor } from '../context/ThemeContext';

interface AuditLogEntryDto {
  id: number;
  username: string;
  actionType: string;
  moduleName: string;
  entityId: string;
  beforeValue: string;
  afterValue: string;
  createdAt: string;
}

export const Administration: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('SYSTEM_WRITE');

  const [activeTab, setActiveTab] = useState<'users' | 'roles' | 'audit' | 'taxRates' | 'settings' | 'appearance'>('users');
  const { themeColor, setThemeColor } = useTheme();

  // Users State
  const [users, setUsers] = useState<UserDto[]>([]);
  const [usrLoading, setUsrLoading] = useState(true);
  const [usrPage, setUsrPage] = useState(0);
  const [usrSize] = useState(10);
  const [usrTotalPages, setUsrTotalPages] = useState(0);
  const [usrTotalElements, setUsrTotalElements] = useState(0);
  const [usrSearch, setUsrSearch] = useState('');
  const [usrStatusFilter, setUsrStatusFilter] = useState('');
  const [isUserModalOpen, setIsUserModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);

  // Roles State
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [roleLoading, setRoleLoading] = useState(true);
  const [rolePage, setRolePage] = useState(0);
  const [roleSize] = useState(10);
  const [roleTotalPages, setRoleTotalPages] = useState(0);
  const [roleTotalElements, setRoleTotalElements] = useState(0);
  const [roleSearch, setRoleSearch] = useState('');
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<RoleDto | null>(null);
  const [isDashboardConfigModalOpen, setIsDashboardConfigModalOpen] = useState(false);
  const [configRole, setConfigRole] = useState<RoleDto | null>(null);

  // Audit State
  const [auditLogs, setAuditLogs] = useState<AuditLogEntryDto[]>([]);
  const [auditLoading, setAuditLoading] = useState(true);
  const [auditPage, setAuditPage] = useState(0);
  const [auditSize] = useState(10);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditTotalElements, setAuditTotalElements] = useState(0);
  const [auditModuleFilter, setAuditModuleFilter] = useState('');
  const [auditUserFilter, setAuditUserFilter] = useState('');
  const [auditActionFilter, setAuditActionFilter] = useState('');
  const [auditStartDate, setAuditStartDate] = useState('');
  const [auditEndDate, setAuditEndDate] = useState('');
  const [selectedAuditLog, setSelectedAuditLog] = useState<AuditLogEntryDto | null>(null);

  // Tax Rates State
  const [taxRates, setTaxRates] = useState<any[]>([]);
  const [taxRatesLoading, setTaxRatesLoading] = useState(true);
  const [activeTaxRateId, setActiveTaxRateId] = useState<string>(() => localStorage.getItem('active_tax_rate_id') || '');
  const [isTaxModalOpen, setIsTaxModalOpen] = useState(false);
  const [taxGroupName, setTaxGroupName] = useState('');
  const [taxComponents, setTaxComponents] = useState<{ name: string; rate: string }[]>([
    { name: 'CGST', rate: '' },
    { name: 'SGST', rate: '' },
  ]);

  const [companyDetails, setCompanyDetails] = useState<CompanyDetails>(() => getStoredCompanyDetails());

  useEffect(() => {
    if (activeTab === 'users') fetchUsers();
    if (activeTab === 'roles') fetchRoles();
    if (activeTab === 'audit') fetchAuditLogs();
    if (activeTab === 'taxRates') fetchTaxRates();
    if (activeTab === 'settings') fetchCompanySettingsFromBackend().then(loaded => setCompanyDetails(loaded));
  }, [activeTab, usrPage, usrStatusFilter, rolePage, auditPage, auditModuleFilter, auditUserFilter, auditActionFilter, auditStartDate, auditEndDate]);

  const fetchTaxRates = async () => {
    setTaxRatesLoading(true);
    try {
      const data = await api.get('/api/lookups/tax-rates');
      const list = data.data || [];
      setTaxRates(list);
      if (list.length > 0) {
        const stored = localStorage.getItem('active_tax_rate_id');
        if (!stored || !list.some((t: any) => String(t.id) === String(stored))) {
          const firstId = String(list[0].id);
          setActiveTaxRateId(firstId);
          localStorage.setItem('active_tax_rate_id', firstId);
        }
      }
    } catch (err) {
      toastEvents.error('Failed to load tax rates');
    } finally {
      setTaxRatesLoading(false);
    }
  };

  const fetchUsers = async () => {
    setUsrLoading(true);
    try {
      const params: any = {};
      if (usrSearch) params.search = usrSearch;
      if (usrStatusFilter) params.status = usrStatusFilter;

      const data = await fetchPaginated<UserDto>('/api/admins/users', { page: usrPage, size: usrSize, sort: 'id,desc', ...params });
      setUsers(data.content || []);
      setUsrTotalPages(data.totalPages || 0);
      setUsrTotalElements(data.totalElements || 0);
    } catch (err) {
      toastEvents.error('Failed to load users');
    } finally {
      setUsrLoading(false);
    }
  };

  const fetchRoles = async () => {
    setRoleLoading(true);
    try {
      const params: any = {};
      if (roleSearch) params.search = roleSearch;

      const data = await fetchPaginated<RoleDto>('/api/admins/roles', { page: rolePage, size: roleSize, sort: 'id,desc', ...params });
      setRoles(data.content || []);
      setRoleTotalPages(data.totalPages || 0);
      setRoleTotalElements(data.totalElements || 0);
    } catch (err) {
      toastEvents.error('Failed to load roles');
    } finally {
      setRoleLoading(false);
    }
  };

  const fetchAuditLogs = async () => {
    setAuditLoading(true);
    try {
      const params: any = {};
      if (auditModuleFilter) params.moduleName = auditModuleFilter;
      if (auditUserFilter) params.username = auditUserFilter;
      if (auditActionFilter) params.actionType = auditActionFilter;
      if (auditStartDate) params.startDate = auditStartDate + 'T00:00:00';
      if (auditEndDate) params.endDate = auditEndDate + 'T23:59:59';

      const data = await fetchPaginated<AuditLogEntryDto>('/api/admins/audit-logs', { page: auditPage, size: auditSize, sort: 'createdAt,desc', ...params });
      setAuditLogs(data.content || []);
      setAuditTotalPages(data.totalPages || 0);
      setAuditTotalElements(data.totalElements || 0);
    } catch (err) {
      toastEvents.error('Failed to load audit logs');
    } finally {
      setAuditLoading(false);
    }
  };

  const handleDeactivateUser = async (id: number) => {
    if (!window.confirm('Are you sure you want to deactivate this user?')) return;
    try {
      await api.delete(`/api/admins/users/${id}`);
      toastEvents.success('User deactivated successfully');
      fetchUsers();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to deactivate user');
    }
  };

  const handleDeactivateRole = async (id: number) => {
    if (!window.confirm('Are you sure you want to deactivate this role?')) return;
    try {
      await api.delete(`/api/admins/roles/${id}`);
      toastEvents.success('Role deactivated successfully');
      fetchRoles();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to deactivate role');
    }
  };

  const getUserInitials = (username: string) => {
    if (!username) return 'US';
    return username.substring(0, 2).toUpperCase();
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon" style={{ backgroundColor: 'rgba(168, 85, 247, 0.12)', color: '#a855f7' }}>
            <Shield size={24} />
          </div>
          <div>
            <h1 className="page-title">System Administration & Security</h1>
            <p className="page-description">
              Manage user accounts, RBAC security roles, audit trail logs, tax lookup tables, and company details.
            </p>
          </div>
        </div>

        <div className="header-actions">
          <button
            onClick={() => {
              if (activeTab === 'users') fetchUsers();
              if (activeTab === 'roles') fetchRoles();
              if (activeTab === 'audit') fetchAuditLogs();
              if (activeTab === 'taxRates') fetchTaxRates();
            }}
            className="btn btn-secondary"
            title="Refresh active view"
          >
            <RefreshCw size={16} /> Refresh
          </button>
        </div>
      </div>

      {/* Metric Stats Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)' }}>
            <Users size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Users</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>{usrTotalElements || users.length}</h3>
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(168, 85, 247, 0.12)', color: '#a855f7' }}>
            <Key size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>RBAC Roles</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>{roleTotalElements || roles.length}</h3>
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <Activity size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Audit Logs</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>{auditTotalElements || auditLogs.length}</h3>
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b' }}>
            <Percent size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Tax Rates</span>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '2px' }}>{taxRates.length}</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="glass-panel mb-4" style={{ padding: '6px', borderRadius: 'var(--border-radius-lg)', display: 'flex', gap: '6px', overflowX: 'auto' }}>
        <button
          className={`btn ${activeTab === 'users' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('users'); setUsrPage(0); }}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Users size={16} /> User Accounts
        </button>
        <button
          className={`btn ${activeTab === 'roles' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('roles'); setRolePage(0); }}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Key size={16} /> Roles & Permissions
        </button>
        <button
          className={`btn ${activeTab === 'audit' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('audit'); setAuditPage(0); }}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Activity size={16} /> Security Audit Trail
        </button>
        <button
          className={`btn ${activeTab === 'taxRates' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('taxRates')}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Percent size={16} /> GST & Tax Rates
        </button>
        <button
          className={`btn ${activeTab === 'settings' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('settings')}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Building2 size={16} /> Company Profile
        </button>
        <button
          className={`btn ${activeTab === 'appearance' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('appearance')}
          style={{ flex: 1, minWidth: '130px', justifyContent: 'center' }}
        >
          <Palette size={16} /> Appearance
        </button>
      </div>

      {/* TAB 1: USERS */}
      {activeTab === 'users' && (
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          {/* Controls */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flex: 1, flexWrap: 'wrap' }}>
              <div style={{ position: 'relative', width: '260px' }}>
                <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="text"
                  placeholder="Search usernames..."
                  className="form-control"
                  style={{ paddingLeft: '36px' }}
                  value={usrSearch}
                  onChange={(e) => { setUsrSearch(e.target.value); setUsrPage(0); }}
                />
              </div>

              <select
                className="form-select"
                style={{ width: '160px' }}
                value={usrStatusFilter}
                onChange={(e) => { setUsrStatusFilter(e.target.value); setUsrPage(0); }}
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="LOCKED">Locked</option>
              </select>
            </div>

            {hasWriteAccess && (
              <button className="btn btn-primary" onClick={() => { setEditingUser(null); setIsUserModalOpen(true); }}>
                <Plus size={16} /> Add New User
              </button>
            )}
          </div>

          {/* Users Data Table */}
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User Profile</th>
                  <th>Username</th>
                  <th>Assigned Roles</th>
                  <th>Failed Logins</th>
                  <th>Status</th>
                  {hasWriteAccess && <th style={{ textAlign: 'right' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {usrLoading ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>Loading user accounts…</td></tr>
                ) : users.length === 0 ? (
                  <tr><td colSpan={hasWriteAccess ? 6 : 5} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>No user accounts found matching criteria.</td></tr>
                ) : users.map(user => {
                  const isActive = user.status === 'ACTIVE';
                  const isLocked = user.status === 'LOCKED';
                  return (
                    <tr key={user.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                          <div style={{
                            width: '36px', height: '36px', borderRadius: '50%',
                            backgroundColor: (user.username === 'admin' || user.username === 'sethu') ? 'rgba(37, 99, 235, 0.15)' : 'var(--bg-tertiary)',
                            color: (user.username === 'admin' || user.username === 'sethu') ? 'var(--accent-primary)' : 'var(--text-primary)',
                            fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            border: '1px solid var(--border-color)'
                          }}>
                            {getUserInitials(user.username)}
                          </div>
                          <div>
                            <span style={{ fontWeight: 700, color: 'var(--text-primary)', display: 'block' }}>
                              #{user.id} {user.username}
                            </span>
                            {(user.username === 'admin' || user.username === 'sethu') && (
                              <span style={{ fontSize: '0.7rem', color: 'var(--accent-primary)', fontWeight: 600 }}>System Administrator</span>
                            )}
                          </div>
                        </div>
                      </td>

                      <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{user.username}</td>

                      <td>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                          {(!user.roles || user.roles.length === 0) ? (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>No roles assigned</span>
                          ) : (
                            user.roles.map(r => (
                              <span
                                key={r.id}
                                style={{
                                  fontSize: '0.72rem',
                                  fontWeight: 700,
                                  padding: '2px 8px',
                                  borderRadius: '12px',
                                  backgroundColor: r.name === 'ROLE_ADMINISTRATOR' ? 'rgba(37, 99, 235, 0.12)' : 'rgba(168, 85, 247, 0.12)',
                                  color: r.name === 'ROLE_ADMINISTRATOR' ? 'var(--accent-primary)' : '#a855f7',
                                  border: `1px solid ${r.name === 'ROLE_ADMINISTRATOR' ? 'rgba(37, 99, 235, 0.2)' : 'rgba(168, 85, 247, 0.2)'}`
                                }}
                              >
                                {r.name}
                              </span>
                            ))
                          )}
                        </div>
                      </td>

                      <td>
                        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: user.failedLoginCount && user.failedLoginCount > 0 ? 'var(--danger)' : 'var(--text-muted)' }}>
                          {user.failedLoginCount || 0}
                        </span>
                      </td>

                      <td>
                        <span
                          style={{
                            fontSize: '0.72rem',
                            fontWeight: 700,
                            padding: '3px 10px',
                            borderRadius: '12px',
                            backgroundColor: isActive ? 'var(--success-bg)' : isLocked ? 'var(--warning-bg)' : 'var(--danger-bg)',
                            color: isActive ? 'var(--success-text)' : isLocked ? 'var(--warning-text)' : 'var(--danger-text)'
                          }}
                        >
                          {user.status}
                        </span>
                      </td>

                      {hasWriteAccess && (
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '6px' }}>
                            <button
                              className="btn btn-outline btn-sm"
                              title="Edit User & Roles"
                              onClick={() => { setEditingUser(user); setIsUserModalOpen(true); }}
                            >
                              <Edit size={14} /> Edit
                            </button>
                            {user.status !== 'INACTIVE' && user.username !== 'admin' && user.username !== 'sethu' && (
                              <button
                                className="btn btn-outline btn-sm"
                                style={{ color: 'var(--danger)' }}
                                title="Deactivate account"
                                onClick={() => handleDeactivateUser(user.id)}
                              >
                                <Trash2 size={14} />
                              </button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {usrTotalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1.25rem' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Showing {usrPage * usrSize + 1} to {Math.min((usrPage + 1) * usrSize, usrTotalElements)} of {usrTotalElements} users
              </span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <button className="btn btn-outline btn-sm" onClick={() => setUsrPage(p => Math.max(0, p - 1))} disabled={usrPage === 0}>
                  <ChevronLeft size={16} /> Prev
                </button>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, padding: '0 0.5rem' }}>
                  Page {usrPage + 1} of {usrTotalPages}
                </span>
                <button className="btn btn-outline btn-sm" onClick={() => setUsrPage(p => Math.min(usrTotalPages - 1, p + 1))} disabled={usrPage === usrTotalPages - 1}>
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: ROLES */}
      {activeTab === 'roles' && (
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ position: 'relative', width: '280px' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Search security roles..."
                className="form-control"
                style={{ paddingLeft: '36px' }}
                value={roleSearch}
                onChange={(e) => { setRoleSearch(e.target.value); setRolePage(0); }}
              />
            </div>

            {hasWriteAccess && (
              <button className="btn btn-primary" onClick={() => { setEditingRole(null); setIsRoleModalOpen(true); }}>
                <Plus size={16} /> Create Security Role
              </button>
            )}
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Role ID</th>
                  <th>Role Identifier</th>
                  <th>Assigned Permissions</th>
                  {hasWriteAccess && <th style={{ textAlign: 'right' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {roleLoading ? (
                  <tr><td colSpan={hasWriteAccess ? 4 : 3} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>Loading security roles…</td></tr>
                ) : roles.length === 0 ? (
                  <tr><td colSpan={hasWriteAccess ? 4 : 3} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>No security roles found.</td></tr>
                ) : roles.map(role => (
                  <tr key={role.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>#{role.id}</td>

                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Key size={16} style={{ color: role.name === 'ROLE_ADMINISTRATOR' ? 'var(--accent-primary)' : '#a855f7' }} />
                        <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{role.name}</span>
                      </div>
                    </td>

                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, padding: '2px 8px', borderRadius: '12px', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)' }}>
                          {role.permissions?.length || 0} Permissions
                        </span>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '3px', maxWidth: '380px' }}>
                          {(role.permissions || []).slice(0, 4).map(p => (
                            <span key={p.id} style={{ fontSize: '0.68rem', padding: '1px 6px', borderRadius: '4px', backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-muted)', border: '1px solid var(--border-color)' }}>
                              {p.code}
                            </span>
                          ))}
                          {(role.permissions || []).length > 4 && (
                            <span style={{ fontSize: '0.68rem', padding: '1px 6px', color: 'var(--text-muted)' }}>
                              +{(role.permissions?.length || 0) - 4} more
                            </span>
                          )}
                        </div>
                      </div>
                    </td>

                    {hasWriteAccess && (
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '6px' }}>
                          <button
                            className="btn btn-outline btn-sm"
                            title="Configure Dashboard Widgets for this Role"
                            onClick={() => { setConfigRole(role); setIsDashboardConfigModalOpen(true); }}
                            style={{ color: '#a855f7' }}
                          >
                            <LayoutDashboard size={14} /> Dashboard
                          </button>
                          <button
                            className="btn btn-outline btn-sm"
                            title="Edit Role Permissions"
                            onClick={() => { setEditingRole(role); setIsRoleModalOpen(true); }}
                          >
                            <Edit size={14} /> Permissions
                          </button>
                          {role.name !== 'ROLE_ADMINISTRATOR' && (
                            <button
                              className="btn btn-outline btn-sm"
                              style={{ color: 'var(--danger)' }}
                              title="Deactivate role"
                              onClick={() => handleDeactivateRole(role.id)}
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {roleTotalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1.25rem' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Showing {rolePage * roleSize + 1} to {Math.min((rolePage + 1) * roleSize, roleTotalElements)} of {roleTotalElements} roles
              </span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <button className="btn btn-outline btn-sm" onClick={() => setRolePage(p => Math.max(0, p - 1))} disabled={rolePage === 0}>
                  <ChevronLeft size={16} /> Prev
                </button>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, padding: '0 0.5rem' }}>
                  Page {rolePage + 1} of {roleTotalPages}
                </span>
                <button className="btn btn-outline btn-sm" onClick={() => setRolePage(p => Math.min(roleTotalPages - 1, p + 1))} disabled={rolePage === roleTotalPages - 1}>
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: AUDIT LOGS */}
      {activeTab === 'audit' && (
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          {/* Filters Bar */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 150px), 1fr))', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <div style={{ position: 'relative' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Username..."
                className="form-control"
                style={{ paddingLeft: '36px' }}
                value={auditUserFilter}
                onChange={(e) => { setAuditUserFilter(e.target.value); setAuditPage(0); }}
              />
            </div>

            <input
              type="date"
              className="form-control"
              value={auditStartDate}
              onChange={(e) => { setAuditStartDate(e.target.value); setAuditPage(0); }}
              title="Start Date"
            />

            <input
              type="date"
              className="form-control"
              value={auditEndDate}
              onChange={(e) => { setAuditEndDate(e.target.value); setAuditPage(0); }}
              title="End Date"
            />

            <select className="form-select" value={auditActionFilter} onChange={(e) => { setAuditActionFilter(e.target.value); setAuditPage(0); }}>
              <option value="">All Actions</option>
              <option value="CREATE_USER">Create User</option>
              <option value="UPDATE_USER">Update User</option>
              <option value="CREATE_ROLE">Create Role</option>
            </select>

            <select className="form-select" value={auditModuleFilter} onChange={(e) => { setAuditModuleFilter(e.target.value); setAuditPage(0); }}>
              <option value="">All Modules</option>
              <option value="ADMIN">Administration</option>
              <option value="FUNDS">Funds</option>
              <option value="SALES">Sales</option>
              <option value="PURCHASING">Purchasing</option>
              <option value="INVENTORY">Inventory</option>
            </select>
          </div>

          {/* Audit Logs Table */}
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>User</th>
                  <th>Action Event</th>
                  <th>Module</th>
                  <th>Target Entity</th>
                  <th style={{ textAlign: 'right' }}>View State</th>
                </tr>
              </thead>
              <tbody>
                {auditLoading ? (
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>Loading security audit logs…</td></tr>
                ) : auditLogs.length === 0 ? (
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>No audit events found matching filters.</td></tr>
                ) : auditLogs.map(log => (
                  <tr key={log.id}>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                      {new Date(log.createdAt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </td>

                    <td style={{ fontWeight: 600, fontSize: '0.85rem' }}>{log.username}</td>

                    <td>
                      <span style={{ fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '12px', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)' }}>
                        {log.actionType}
                      </span>
                    </td>

                    <td style={{ fontSize: '0.85rem' }}>{log.moduleName}</td>

                    <td style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {log.entityId || '—'}
                    </td>

                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-outline btn-sm"
                        onClick={() => setSelectedAuditLog(log)}
                        title="View Full Action Diff / State Details"
                      >
                        <Eye size={14} /> Diff JSON
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {auditTotalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1.25rem' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Showing {auditPage * auditSize + 1} to {Math.min((auditPage + 1) * auditSize, auditTotalElements)} of {auditTotalElements} events
              </span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <button className="btn btn-outline btn-sm" onClick={() => setAuditPage(p => Math.max(0, p - 1))} disabled={auditPage === 0}>
                  <ChevronLeft size={16} /> Prev
                </button>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, padding: '0 0.5rem' }}>
                  Page {auditPage + 1} of {auditTotalPages}
                </span>
                <button className="btn btn-outline btn-sm" onClick={() => setAuditPage(p => Math.min(auditTotalPages - 1, p + 1))} disabled={auditPage === auditTotalPages - 1}>
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 4: GST & TAX RATES */}
      {activeTab === 'taxRates' && (
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
            <div>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>GST & Tax Rate Configuration</h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
                Configure GST slab splits (CGST, SGST, IGST) used across billing and invoice calculations.
              </p>
            </div>

            {hasWriteAccess && (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setTaxGroupName('');
                  setTaxComponents([{ name: 'CGST', rate: '' }, { name: 'SGST', rate: '' }]);
                  setIsTaxModalOpen(true);
                }}
              >
                <Plus size={16} /> Add Tax Rate
              </button>
            )}
          </div>

          {taxRates.length > 0 && (
            <div className="glass-panel" style={{ padding: '1rem 1.25rem', marginBottom: '1.25rem', backgroundColor: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.25)', borderRadius: 'var(--border-radius)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <h4 style={{ margin: 0, fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <CheckCircle2 size={18} color="#10b981" /> Active Default Billing Tax Rule
                  </h4>
                  <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Selected tax rule below will automatically be used for ALL invoices created in Billing.
                  </p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Active Billing Tax Rule:</label>
                  <select
                    value={activeTaxRateId}
                    onChange={(e) => {
                      setActiveTaxRateId(e.target.value);
                      localStorage.setItem('active_tax_rate_id', e.target.value);
                      toastEvents.success('Active billing tax rule updated');
                    }}
                    className="form-control"
                    style={{ fontWeight: 800, minWidth: '220px', backgroundColor: 'var(--bg-primary)', color: 'var(--text-primary)' }}
                  >
                    {taxRates.map(t => (
                      <option key={t.id} value={String(t.id)}>
                        {t.name} ({t.rate}%)
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 260px), 1fr))', gap: '1rem' }}>
            {taxRatesLoading ? (
              <div style={{ gridColumn: '1 / -1', padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                Loading tax rates…
              </div>
            ) : taxRates.length === 0 ? (
              <div style={{ gridColumn: '1 / -1', padding: '3rem', textAlign: 'center', color: 'var(--text-muted)', border: '1px dashed var(--border-color)', borderRadius: 'var(--border-radius)' }}>
                <Percent size={40} style={{ margin: '0 auto 0.75rem', opacity: 0.3 }} />
                <p style={{ fontSize: '1rem', fontWeight: 600 }}>No tax rates defined.</p>
                <p style={{ fontSize: '0.8rem', marginTop: '4px' }}>Click <strong>Add Tax Rate</strong> above to configure your first tax rate.</p>
              </div>
            ) : (
              taxRates.map(tax => {
                let comps: { name: string; rate: number }[] = [];
                if (tax.componentsJson) {
                  try {
                    const parsed = JSON.parse(tax.componentsJson);
                    if (Array.isArray(parsed) && parsed.length > 0) {
                      comps = parsed;
                    }
                  } catch (e) {}
                }
                if (comps.length === 0) {
                  if ((tax.cgstRate || 0) > 0) comps.push({ name: 'CGST', rate: tax.cgstRate });
                  if ((tax.sgstRate || 0) > 0) comps.push({ name: 'SGST', rate: tax.sgstRate });
                  if ((tax.igstRate || 0) > 0) comps.push({ name: 'IGST', rate: tax.igstRate });
                  const splitTotal = (tax.cgstRate || 0) + (tax.sgstRate || 0) + (tax.igstRate || 0);
                  if (splitTotal === 0 && (tax.rate || 0) > 0) comps.push({ name: 'Total Rate', rate: tax.rate });
                }

                const isActive = String(tax.id) === String(activeTaxRateId);

                return (
                  <div
                    key={tax.id}
                    className="glass-panel"
                    style={{
                      padding: '1.25rem',
                      borderRadius: 'var(--border-radius)',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      gap: '1rem',
                      borderLeft: isActive ? '4px solid #10b981' : '4px solid var(--accent-primary)',
                      backgroundColor: isActive ? 'rgba(16, 185, 129, 0.04)' : undefined
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                              {tax.name}
                            </h3>
                            {isActive && (
                              <span style={{ fontSize: '0.65rem', fontWeight: 800, padding: '2px 6px', borderRadius: '4px', backgroundColor: '#10b981', color: '#fff', textTransform: 'uppercase' }}>
                                Active for Billing
                              </span>
                            )}
                          </div>
                          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                            Total Tax: {tax.rate}%
                          </span>
                        </div>

                        {hasWriteAccess && (
                          <button
                            onClick={async (e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              try {
                                await api.delete(`/api/lookups/tax-rates/${tax.id}`);
                                toastEvents.success(`Tax rate "${tax.name}" deleted successfully`);
                                if (String(tax.id) === String(activeTaxRateId)) {
                                  localStorage.removeItem('active_tax_rate_id');
                                  setActiveTaxRateId('');
                                }
                                fetchTaxRates();
                              } catch (err: any) {
                                console.error('Delete tax rate error:', err);
                                const msg = err.response?.data?.message || err.message || 'Failed to delete tax rate';
                                toastEvents.error(msg);
                              }
                            }}
                            className="btn btn-outline btn-sm"
                            style={{ color: 'var(--danger)', padding: '4px 6px', zIndex: 5 }}
                            title="Delete tax rate"
                          >
                            <Trash2 size={14} />
                          </button>
                        )}
                      </div>

                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '0.85rem' }}>
                        {comps.map((c, i) => (
                          <span
                            key={i}
                            style={{
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              padding: '3px 10px',
                              borderRadius: '12px',
                              backgroundColor: 'rgba(37, 99, 235, 0.1)',
                              color: 'var(--accent-primary)',
                              border: '1px solid rgba(37, 99, 235, 0.2)'
                            }}
                          >
                            {c.name}: {c.rate}%
                          </span>
                        ))}
                      </div>
                    </div>

                    {!isActive && (
                      <div style={{ paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)' }}>
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTaxRateId(String(tax.id));
                            localStorage.setItem('active_tax_rate_id', String(tax.id));
                            toastEvents.success(`Set "${tax.name}" as active rule for all billing`);
                          }}
                          className="btn btn-outline btn-sm"
                          style={{ width: '100%', fontSize: '0.75rem', fontWeight: 700 }}
                        >
                          Set as Active Rule for Billing
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* TAB 5: SYSTEM & COMPANY SETTINGS */}
      {activeTab === 'settings' && (
        <CompanySettings 
          currentSettings={companyDetails} 
          onSettingsChanged={(newDetails) => {
            setCompanyDetails(newDetails);
            toastEvents.success('Company profile updated successfully');
          }} 
        />
      )}

      {/* TAB 6: APPEARANCE / THEME */}
      {activeTab === 'appearance' && (
        <div className="glass-panel" style={{ padding: '2rem', borderRadius: 'var(--border-radius-lg)', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.5rem 0', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Palette size={20} className="text-blue-500" /> User Interface Personalization
            </h2>
            <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
              Customize the look and feel of SMC Management. Your preferences are saved locally for this device.
            </p>
          </div>

          <div style={{ border: '1px solid var(--border-color)', borderRadius: 'var(--border-radius-lg)', padding: '1.5rem', backgroundColor: 'var(--bg-secondary)' }}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 1rem 0' }}>Primary Brand Color</h3>
            
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              {(Object.entries(THEME_COLORS) as [ThemeColor, typeof THEME_COLORS[ThemeColor]][]).map(([key, color]) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setThemeColor(key)}
                  title={color.name}
                  style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '50%',
                    background: color.gradient,
                    border: themeColor === key ? '3px solid var(--text-primary)' : '2px solid transparent',
                    boxShadow: themeColor === key ? '0 0 0 3px var(--bg-secondary) inset, var(--shadow-sm)' : 'var(--shadow-sm)',
                    cursor: 'pointer',
                    transition: 'var(--transition-smooth)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  {themeColor === key && <CheckCircle2 size={24} color="#fff" />}
                </button>
              ))}
            </div>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '1rem', marginBottom: 0 }}>
              This color will be used for primary buttons, active tabs, and highlights across the application.
            </p>
          </div>
        </div>
      )}

      {/* Audit Log Detail JSON Modal */}
      {selectedAuditLog && (
        <div className="modal-backdrop" style={{ zIndex: 1000 }}>
          <div className="glass-panel" style={{ width: '640px', maxWidth: '95vw', padding: '1.75rem', borderRadius: 'var(--border-radius-lg)', boxShadow: 'var(--shadow-lg)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ padding: '0.5rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)' }}>
                  <FileJson size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>
                    Audit Event Details #{selectedAuditLog.id}
                  </h3>
                  <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
                    {selectedAuditLog.actionType} by {selectedAuditLog.username} on {new Date(selectedAuditLog.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
              <button onClick={() => setSelectedAuditLog(null)} className="btn btn-outline btn-sm" style={{ padding: '6px', borderRadius: '50%' }}>
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label className="form-label" style={{ fontWeight: 600 }}>Action State Value / After Value</label>
                <pre style={{
                  backgroundColor: 'var(--bg-tertiary)',
                  color: 'var(--text-primary)',
                  padding: '1rem',
                  borderRadius: 'var(--border-radius-sm)',
                  border: '1px solid var(--border-color)',
                  fontSize: '0.8rem',
                  fontFamily: 'monospace',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                  maxHeight: '260px',
                  overflowY: 'auto'
                }}>
                  {selectedAuditLog.afterValue || selectedAuditLog.beforeValue || 'No state data captured.'}
                </pre>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '0.5rem' }}>
                <button className="btn btn-secondary" onClick={() => setSelectedAuditLog(null)}>
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tax Rate Modal */}
      {isTaxModalOpen && (() => {
        const totalRate = taxComponents.reduce((sum, c) => sum + (parseFloat(c.rate) || 0), 0);
        const cgst = taxComponents.filter(c => c.name.toUpperCase().includes('CGST')).reduce((s, c) => s + (parseFloat(c.rate) || 0), 0);
        const sgst = taxComponents.filter(c => c.name.toUpperCase().includes('SGST')).reduce((s, c) => s + (parseFloat(c.rate) || 0), 0);
        const igst = taxComponents.filter(c => c.name.toUpperCase().includes('IGST')).reduce((s, c) => s + (parseFloat(c.rate) || 0), 0);
        return (
          <div className="modal-backdrop" style={{ zIndex: 1000 }}>
            <div className="glass-panel" style={{ width: '500px', maxWidth: '95vw', padding: '1.75rem', borderRadius: 'var(--border-radius-lg)', boxShadow: 'var(--shadow-lg)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ padding: '0.5rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b' }}>
                    <Percent size={20} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>Create Tax Rate</h3>
                    <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
                      Specify GST rate name and split components.
                    </p>
                  </div>
                </div>
                <button onClick={() => setIsTaxModalOpen(false)} className="btn btn-outline btn-sm" style={{ padding: '6px', borderRadius: '50%' }}>
                  <X size={18} />
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div className="form-group" style={{ margin: 0 }}>
                  <label className="form-label" style={{ fontWeight: 600 }}>Tax Name *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. GST 18%, IGST 12%"
                    value={taxGroupName}
                    onChange={e => setTaxGroupName(e.target.value)}
                  />
                </div>

                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <label className="form-label" style={{ fontWeight: 600, margin: 0 }}>Tax Components</label>
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => setTaxComponents(prev => [...prev, { name: '', rate: '' }])}
                    >
                      + Add Row
                    </button>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {taxComponents.map((comp, idx) => (
                      <div key={idx} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Component (e.g. CGST, SGST)"
                          value={comp.name}
                          onChange={e => setTaxComponents(prev => prev.map((c, i) => i === idx ? { ...c, name: e.target.value } : c))}
                          style={{ flex: 1 }}
                        />
                        <div style={{ position: 'relative', width: '110px' }}>
                          <input
                            type="number"
                            className="form-control"
                            placeholder="%"
                            min="0"
                            step="0.01"
                            value={comp.rate}
                            onChange={e => setTaxComponents(prev => prev.map((c, i) => i === idx ? { ...c, rate: e.target.value } : c))}
                            style={{ paddingRight: '22px' }}
                          />
                          <span style={{ position: 'absolute', right: '8px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>%</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => setTaxComponents(prev => prev.filter((_, i) => i !== idx))}
                          className="btn btn-outline btn-sm"
                          style={{ color: 'var(--danger)', padding: '6px' }}
                          disabled={taxComponents.length <= 1}
                        >
                          <X size={14} />
                        </button>
                      </div>
                    ))}
                  </div>

                  {totalRate > 0 && (
                    <div style={{ marginTop: '0.75rem', padding: '0.75rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(37, 99, 235, 0.08)', border: '1px solid rgba(37, 99, 235, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Combined Rate Total</span>
                      <span style={{ fontWeight: 800, color: 'var(--accent-primary)', fontSize: '1.1rem' }}>{totalRate.toFixed(2)}%</span>
                    </div>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', marginTop: '1.25rem' }}>
                <button className="btn btn-secondary" onClick={() => setIsTaxModalOpen(false)}>Cancel</button>
                <button className="btn btn-primary" onClick={async () => {
                  if (!taxGroupName.trim()) {
                    toastEvents.error('Please enter a tax name');
                    return;
                  }
                  if (taxComponents.some(c => !c.name.trim() || !c.rate)) {
                    toastEvents.error('Please fill in all component names and rates');
                    return;
                  }
                  const formattedComps = taxComponents.map(c => ({
                    name: c.name.trim(),
                    rate: parseFloat(c.rate) || 0
                  }));
                  const payload = {
                    name: taxGroupName.trim(),
                    rate: totalRate,
                    cgstRate: cgst,
                    sgstRate: sgst,
                    igstRate: igst,
                    componentsJson: JSON.stringify(formattedComps)
                  };
                  try {
                    await api.post('/api/lookups/tax-rates', payload);
                    toastEvents.success('Tax rate created successfully');
                    setIsTaxModalOpen(false);
                    setTaxGroupName('');
                    setTaxComponents([{ name: 'CGST', rate: '' }, { name: 'SGST', rate: '' }]);
                    fetchTaxRates();
                  } catch (err: any) {
                    toastEvents.error(err.response?.data?.message || 'Failed to create tax rate');
                  }
                }}>
                  Save Tax Rate
                </button>
              </div>
            </div>
          </div>
        );
      })()}

      <UserModal
        isOpen={isUserModalOpen}
        onClose={() => setIsUserModalOpen(false)}
        user={editingUser}
        onSaved={fetchUsers}
      />

      <RoleModal
        isOpen={isRoleModalOpen}
        onClose={() => setIsRoleModalOpen(false)}
        role={editingRole}
        onSaved={fetchRoles}
      />

      <DashboardConfigModal
        isOpen={isDashboardConfigModalOpen}
        onClose={() => setIsDashboardConfigModalOpen(false)}
        role={configRole}
      />
    </div>
  );
};
