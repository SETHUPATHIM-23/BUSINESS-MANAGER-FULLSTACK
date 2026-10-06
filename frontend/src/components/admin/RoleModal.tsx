import React, { useState, useEffect, useMemo } from 'react';
import { X, Save, AlertCircle, Key, ShieldCheck, Search } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';

export interface PermissionDto {
  id: number;
  code: string;
  description: string;
}

export interface RoleDto {
  id: number;
  name: string;
  permissions: PermissionDto[];
}

interface RoleModalProps {
  isOpen: boolean;
  onClose: () => void;
  role?: RoleDto | null;
  onSaved: () => void;
}

export const RoleModal: React.FC<RoleModalProps> = ({ isOpen, onClose, role, onSaved }) => {
  const [formData, setFormData] = useState({
    name: '',
    permissionIds: [] as number[]
  });
  
  const [availablePermissions, setAvailablePermissions] = useState<PermissionDto[]>([]);
  const [permSearch, setPermSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState<string>('ALL');
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      fetchPermissions();
      if (role) {
        setFormData({
          name: role.name,
          permissionIds: role.permissions?.map(p => p.id) || []
        });
      } else {
        setFormData({
          name: '',
          permissionIds: []
        });
      }
      setServerErrors({});
      setPermSearch('');
      setActiveCategory('ALL');
    }
  }, [isOpen, role]);

  const fetchPermissions = async () => {
    try {
      const res = await api.get('/api/admins/permissions?size=1000');
      setAvailablePermissions(res.data.content || []);
    } catch (err) {
      toastEvents.error('Failed to load system permissions');
    }
  };

  // Extract permission categories dynamically (e.g., BILLING_READ -> BILLING)
  const categories = useMemo(() => {
    const cats = new Set<string>();
    availablePermissions.forEach(p => {
      const parts = p.code.split('_');
      if (parts.length > 1) {
        cats.add(parts[0]);
      } else {
        cats.add('OTHER');
      }
    });
    return ['ALL', ...Array.from(cats).sort()];
  }, [availablePermissions]);

  const filteredPermissions = useMemo(() => {
    return availablePermissions.filter(p => {
      const matchesSearch = !permSearch || p.code.toLowerCase().includes(permSearch.toLowerCase()) || (p.description && p.description.toLowerCase().includes(permSearch.toLowerCase()));
      const matchesCat = activeCategory === 'ALL' || p.code.startsWith(activeCategory + '_');
      return matchesSearch && matchesCat;
    });
  }, [availablePermissions, permSearch, activeCategory]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (serverErrors[name]) {
      setServerErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handlePermissionToggle = (permissionId: number) => {
    if (role?.name === 'ROLE_ADMINISTRATOR') return;
    setFormData(prev => {
      const isSelected = prev.permissionIds.includes(permissionId);
      return {
        ...prev,
        permissionIds: isSelected ? prev.permissionIds.filter(id => id !== permissionId) : [...prev.permissionIds, permissionId]
      };
    });
  };

  const handleSelectAllCategory = () => {
    if (role?.name === 'ROLE_ADMINISTRATOR') return;
    const catIds = filteredPermissions.map(p => p.id);
    const allSelected = catIds.every(id => formData.permissionIds.includes(id));
    if (allSelected) {
      setFormData(prev => ({ ...prev, permissionIds: prev.permissionIds.filter(id => !catIds.includes(id)) }));
    } else {
      setFormData(prev => ({ ...prev, permissionIds: Array.from(new Set([...prev.permissionIds, ...catIds])) }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setServerErrors({});

    const errors: Record<string, string> = {};
    if (!formData.name || formData.name.trim() === '') {
      errors.name = 'Role name is required';
    }

    if (Object.keys(errors).length > 0) {
      setServerErrors(errors);
      setSaving(false);
      return;
    }

    try {
      const payload = {
        name: formData.name.trim(),
        permissionIds: formData.permissionIds
      };

      if (role) {
        await api.put(`/api/admins/roles/${role.id}`, payload);
        toastEvents.success(`Role '${formData.name}' updated successfully`);
      } else {
        await api.post('/api/admins/roles', payload);
        toastEvents.success(`Role '${formData.name}' created successfully`);
      }
      onSaved();
      onClose();
    } catch (err: any) {
      if (err.response?.data?.fieldErrors) {
        const errors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          errors[fe.field] = fe.message;
        });
        setServerErrors(errors);
      } else if (err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error(role ? 'Failed to update role' : 'Failed to create role');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" style={{ zIndex: 1000 }}>
      <div className="glass-panel" style={{ width: '740px', maxWidth: '95vw', padding: '1.75rem', borderRadius: 'var(--border-radius-lg)', boxShadow: 'var(--shadow-lg)' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ width: '38px', height: '38px', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(168, 85, 247, 0.12)', color: '#a855f7', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Key size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                {role ? `Configure Role: ${role.name}` : 'Create Security Role'}
              </h2>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
                Define security privileges and operational permissions for this user role.
              </p>
            </div>
          </div>
          <button onClick={onClose} className="btn btn-outline btn-sm" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Role Name */}
          <div className="form-group" style={{ margin: 0 }}>
            <label className="form-label" style={{ fontWeight: 600 }}>Role Identifier Name *</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="e.g. ROLE_BILLING_CLERK"
              className={`form-control ${serverErrors.name ? 'error' : ''}`}
              disabled={role?.name === 'ROLE_ADMINISTRATOR'}
              required
            />
            {serverErrors.name && (
              <span style={{ fontSize: '0.75rem', color: 'var(--danger)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <AlertCircle size={12} /> {serverErrors.name}
              </span>
            )}
          </div>

          {/* Permissions Matrix */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.6rem' }}>
              <label className="form-label" style={{ fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '6px' }}>
                <ShieldCheck size={16} style={{ color: 'var(--accent-primary)' }} /> Assigned Module Permissions
              </label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <button
                  type="button"
                  onClick={handleSelectAllCategory}
                  className="btn btn-outline btn-sm"
                  style={{ fontSize: '0.75rem', padding: '3px 8px' }}
                  disabled={role?.name === 'ROLE_ADMINISTRATOR'}
                >
                  Toggle All Visible
                </button>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--accent-primary)', backgroundColor: 'rgba(37, 99, 235, 0.1)', padding: '2px 8px', borderRadius: '12px' }}>
                  {formData.permissionIds.length} Selected
                </span>
              </div>
            </div>

            {/* Filter controls & categories */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.6rem', flexWrap: 'wrap' }}>
              <div style={{ position: 'relative', flex: 1, minWidth: '180px' }}>
                <Search size={14} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  type="text"
                  placeholder="Filter permissions..."
                  value={permSearch}
                  onChange={(e) => setPermSearch(e.target.value)}
                  className="form-control"
                  style={{ paddingLeft: '30px', fontSize: '0.8rem', height: '32px' }}
                />
              </div>

              {/* Category Pills */}
              <div style={{ display: 'flex', gap: '4px', overflowX: 'auto', paddingBottom: '2px', maxWidth: '100%' }}>
                {categories.map(cat => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setActiveCategory(cat)}
                    style={{
                      padding: '3px 9px',
                      fontSize: '0.72rem',
                      fontWeight: 600,
                      borderRadius: '12px',
                      border: '1px solid var(--border-color)',
                      backgroundColor: activeCategory === cat ? 'var(--accent-primary)' : 'var(--bg-tertiary)',
                      color: activeCategory === cat ? '#ffffff' : 'var(--text-secondary)',
                      cursor: 'pointer',
                      whiteSpace: 'nowrap'
                    }}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Permissions Grid */}
            <div style={{ maxHeight: '240px', overflowY: 'auto', padding: '0.75rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--border-radius-sm)', border: '1px solid var(--border-color)', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '0.5rem' }}>
              {filteredPermissions.length === 0 ? (
                <div style={{ gridColumn: '1 / -1', fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', padding: '1.5rem' }}>
                  No matching permissions found.
                </div>
              ) : (
                filteredPermissions.map(perm => {
                  const isChecked = formData.permissionIds.includes(perm.id);
                  return (
                    <div
                      key={perm.id}
                      onClick={() => handlePermissionToggle(perm.id)}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.5rem',
                        padding: '0.5rem',
                        borderRadius: 'var(--border-radius-sm)',
                        backgroundColor: isChecked ? 'rgba(37, 99, 235, 0.08)' : 'var(--bg-secondary)',
                        border: `1px solid ${isChecked ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                        cursor: role?.name === 'ROLE_ADMINISTRATOR' ? 'not-allowed' : 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        disabled={role?.name === 'ROLE_ADMINISTRATOR'}
                        style={{ marginTop: '2px', accentColor: 'var(--accent-primary)' }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', wordBreak: 'break-all' }}>
                          {perm.code}
                        </span>
                        {perm.description && (
                          <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block', lineHeight: 1.2, marginTop: '2px' }}>
                            {perm.description}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Action Buttons */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', marginTop: '0.5rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={16} /> {saving ? 'Saving…' : role ? 'Update Role' : 'Create Role'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
