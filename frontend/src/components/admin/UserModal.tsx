import React, { useState, useEffect } from 'react';
import { X, Save, AlertCircle, User, Shield, CheckCircle2 } from 'lucide-react';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';

export interface RoleDto {
  id: number;
  name: string;
}

export interface UserDto {
  id: number;
  username: string;
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  roles: RoleDto[];
  failedLoginCount?: number;
}

interface UserModalProps {
  isOpen: boolean;
  onClose: () => void;
  user?: UserDto | null;
  onSaved: () => void;
}

export const UserModal: React.FC<UserModalProps> = ({ isOpen, onClose, user, onSaved }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    status: 'ACTIVE',
    roleIds: [] as number[]
  });
  
  const [availableRoles, setAvailableRoles] = useState<RoleDto[]>([]);
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      fetchRoles();
      if (user) {
        setFormData({
          username: user.username,
          password: '',
          status: user.status,
          roleIds: user.roles?.map(r => r.id) || []
        });
      } else {
        setFormData({
          username: '',
          password: '',
          status: 'ACTIVE',
          roleIds: []
        });
      }
      setServerErrors({});
    }
  }, [isOpen, user]);

  const fetchRoles = async () => {
    try {
      const res = await api.get('/api/admins/roles?size=100');
      setAvailableRoles(res.data.content || []);
    } catch (err) {
      toastEvents.error('Failed to load system roles');
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (serverErrors[name]) {
      setServerErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleRoleToggle = (roleId: number) => {
    setFormData(prev => {
      const isSelected = prev.roleIds.includes(roleId);
      return {
        ...prev,
        roleIds: isSelected ? prev.roleIds.filter(id => id !== roleId) : [...prev.roleIds, roleId]
      };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setServerErrors({});

    const errors: Record<string, string> = {};
    if (!formData.username || formData.username.trim() === '') {
      errors.username = 'Username is required';
    } else if (!/^[a-zA-Z0-9_]{3,20}$/.test(formData.username)) {
      errors.username = 'Username must be 3-20 characters long and contain only letters, numbers, and underscores';
    }

    if (!user && !formData.password) {
      errors.password = 'Password is required for new users';
    }

    if (formData.password) {
       if (formData.password.length < 8) errors.password = 'Password must be at least 8 characters long';
       else if (!/.*[A-Z].*/.test(formData.password)) errors.password = 'Password must contain at least one uppercase letter';
       else if (!/.*[a-z].*/.test(formData.password)) errors.password = 'Password must contain at least one lowercase letter';
       else if (!/.*\d.*/.test(formData.password)) errors.password = 'Password must contain at least one number';
       else if (!/.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?].*/.test(formData.password)) errors.password = 'Password must contain at least one special character';
    }

    if (Object.keys(errors).length > 0) {
      setServerErrors(errors);
      setSaving(false);
      return;
    }

    try {
      const payload = {
        username: formData.username.trim(),
        password: formData.password || undefined,
        status: formData.status,
        roleIds: formData.roleIds
      };

      if (user) {
        await api.put(`/api/admins/users/${user.id}`, payload);
        toastEvents.success(`User '${formData.username}' updated successfully`);
      } else {
        await api.post('/api/admins/users', payload);
        toastEvents.success(`User '${formData.username}' created successfully`);
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
        toastEvents.error(user ? 'Failed to update user' : 'Failed to create user');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" style={{ zIndex: 1000 }}>
      <div className="glass-panel" style={{ width: '560px', maxWidth: '95vw', padding: '1.75rem', borderRadius: 'var(--border-radius-lg)', boxShadow: 'var(--shadow-lg)' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ width: '38px', height: '38px', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <User size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                {user ? `Edit User: ${user.username}` : 'Create New User'}
              </h2>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
                {user ? 'Update authentication credentials & assigned security roles.' : 'Add a new operator or administrator account to the system.'}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="btn btn-outline btn-sm" style={{ padding: '6px', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Username & Status Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontWeight: 600 }}>Username *</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="e.g. john_doe"
                  className={`form-control ${serverErrors.username ? 'error' : ''}`}
                  disabled={user?.username === 'admin' || user?.username === 'sethu'}
                  required
                />
              </div>
              {serverErrors.username && (
                <span style={{ fontSize: '0.75rem', color: 'var(--danger)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <AlertCircle size={12} /> {serverErrors.username}
                </span>
              )}
            </div>

            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label" style={{ fontWeight: 600 }}>Account Status *</label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="form-select"
                disabled={user?.username === 'admin' || user?.username === 'sethu'}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive / Disabled</option>
                <option value="LOCKED">Locked</option>
              </select>
            </div>
          </div>

          {/* Password Input */}
          <div className="form-group" style={{ margin: 0 }}>
            <label className="form-label" style={{ fontWeight: 600 }}>
              Password {user ? <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(Leave empty to retain current password)</span> : '*'}
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder={user ? '••••••••' : 'Min 8 chars, 1 uppercase, 1 number, 1 symbol'}
                className={`form-control ${serverErrors.password ? 'error' : ''}`}
                required={!user}
              />
            </div>
            {serverErrors.password && (
              <span style={{ fontSize: '0.75rem', color: 'var(--danger)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <AlertCircle size={12} /> {serverErrors.password}
              </span>
            )}
          </div>

          {/* Assign Roles Section */}
          <div className="form-group" style={{ margin: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <label className="form-label" style={{ fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Shield size={16} style={{ color: 'var(--accent-primary)' }} /> Security Roles
              </label>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                {formData.roleIds.length} role(s) selected
              </span>
            </div>

            <div style={{ maxHeight: '180px', overflowY: 'auto', padding: '0.75rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--border-radius-sm)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {availableRoles.length === 0 ? (
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>Loading roles…</div>
              ) : (
                availableRoles.map(role => {
                  const isChecked = formData.roleIds.includes(role.id);
                  const isPrimaryAdmin = user?.username === 'admin' || user?.username === 'sethu';
                  return (
                    <label
                      key={role.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.5rem 0.75rem',
                        borderRadius: 'var(--border-radius-sm)',
                        backgroundColor: isChecked ? 'rgba(37, 99, 235, 0.08)' : 'var(--bg-secondary)',
                        border: `1px solid ${isChecked ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                        cursor: role.name === 'ROLE_ADMINISTRATOR' && isPrimaryAdmin ? 'not-allowed' : 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleRoleToggle(role.id)}
                          disabled={role.name === 'ROLE_ADMINISTRATOR' && isPrimaryAdmin}
                          style={{ width: '16px', height: '16px', accentColor: 'var(--accent-primary)' }}
                        />
                        <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                          {role.name}
                        </span>
                      </div>
                      {isChecked && <CheckCircle2 size={16} style={{ color: 'var(--accent-primary)' }} />}
                    </label>
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
              <Save size={16} /> {saving ? 'Saving…' : user ? 'Save Changes' : 'Create User'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
