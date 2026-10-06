import React, { useState, useEffect } from 'react';
import { X, AlertCircle, Save, Truck as TruckIcon } from 'lucide-react';

export interface TruckDto {
  id: number;
  registrationNumber: string;
  make: string;
  model: string;
  capacity: number | null;
  fuelType: string | null;
  driverEmployeeId: number | null;
  driverName?: string | null;
  driverCode?: string | null;
  lastServiceDate: string | null;
  maintenanceDue: boolean;
  isOptimistic?: boolean;
}

export interface EmployeeOption {
  id: number;
  name: string;
  employeeCode: string;
}

export interface TruckFormData {
  registrationNumber: string;
  make: string;
  model: string;
  capacity: string;
  fuelType: string;
  driverEmployeeId: string;
  lastServiceDate: string;
}

export interface TruckFormModalProps {
  isOpen: boolean;
  mode: 'create' | 'edit';
  truck: TruckDto | null;
  allDrivers: EmployeeOption[];
  isSubmitting: boolean;
  serverErrors?: Record<string, string>;
  serverGeneralError?: string | null;
  onClose: () => void;
  onSubmit: (formData: TruckFormData) => void;
}

export const TruckFormModal: React.FC<TruckFormModalProps> = ({
  isOpen,
  mode,
  truck,
  allDrivers,
  isSubmitting,
  serverErrors = {},
  serverGeneralError = null,
  onClose,
  onSubmit
}) => {
  const [formData, setFormData] = useState<TruckFormData>({
    registrationNumber: '',
    make: '',
    model: '',
    capacity: '',
    fuelType: 'DIESEL',
    driverEmployeeId: '',
    lastServiceDate: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && truck) {
        setFormData({
          registrationNumber: truck.registrationNumber || '',
          make: truck.make || '',
          model: truck.model || '',
          capacity: truck.capacity != null ? truck.capacity.toString() : '',
          fuelType: truck.fuelType || 'DIESEL',
          driverEmployeeId: truck.driverEmployeeId != null ? truck.driverEmployeeId.toString() : '',
          lastServiceDate: truck.lastServiceDate || ''
        });
      } else {
        setFormData({
          registrationNumber: '',
          make: '',
          model: '',
          capacity: '',
          fuelType: 'DIESEL',
          driverEmployeeId: '',
          lastServiceDate: ''
        });
      }
      setErrors({});
      setTouched({});
    }
  }, [isOpen, mode, truck]);

  // Sync server field errors when passed down from API error response
  useEffect(() => {
    if (serverErrors && Object.keys(serverErrors).length > 0) {
      setErrors(prev => ({ ...prev, ...serverErrors }));
    }
  }, [serverErrors]);

  if (!isOpen) return null;

  // Validation function mirroring backend Bean Validation rules (TruckCreateRequest & TruckUpdateRequest)
  const validateField = (name: keyof TruckFormData, value: string): string => {
    switch (name) {
      case 'registrationNumber':
        if (!value.trim()) return 'Registration number is required';
        return '';
      case 'make':
        if (!value.trim()) return 'Make is required';
        return '';
      case 'model':
        if (!value.trim()) return 'Model is required';
        return '';
      case 'capacity':
        if (value.trim() !== '') {
          const num = parseFloat(value);
          if (isNaN(num) || num <= 0) {
            return 'Capacity must be a positive number';
          }
        }
        return '';
      default:
        return '';
    }
  };

  const validateAll = (): Record<string, string> => {
    const newErrors: Record<string, string> = {};
    const regErr = validateField('registrationNumber', formData.registrationNumber);
    if (regErr) newErrors.registrationNumber = regErr;

    const makeErr = validateField('make', formData.make);
    if (makeErr) newErrors.make = makeErr;

    const modelErr = validateField('model', formData.model);
    if (modelErr) newErrors.model = modelErr;

    const capErr = validateField('capacity', formData.capacity);
    if (capErr) newErrors.capacity = capErr;

    return newErrors;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));

    if (touched[name]) {
      const errorMsg = validateField(name as keyof TruckFormData, value);
      setErrors(prev => ({
        ...prev,
        [name]: errorMsg
      }));
    }
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setTouched(prev => ({ ...prev, [name]: true }));
    const errorMsg = validateField(name as keyof TruckFormData, value);
    setErrors(prev => ({
      ...prev,
      [name]: errorMsg
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Mark all inputs as touched
    setTouched({
      registrationNumber: true,
      make: true,
      model: true,
      capacity: true,
      fuelType: true,
      driverEmployeeId: true,
      lastServiceDate: true
    });

    const clientValidationErrors = validateAll();
    if (Object.keys(clientValidationErrors).length > 0) {
      setErrors(clientValidationErrors);
      return;
    }

    setErrors({});
    onSubmit(formData);
  };

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0, 0, 0, 0.65)', display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 1000, backdropFilter: 'blur(4px)', padding: '1rem'
    }}>
      <div className="glass-panel" style={{
        width: '100%', maxWidth: '560px', background: 'var(--bg-card, #111827)',
        padding: '2rem', borderRadius: '24px',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)',
        border: '1px solid var(--border-color, rgba(255,255,255,0.1))'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color, rgba(255,255,255,0.1))', paddingBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ padding: '8px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.1)', color: 'var(--primary-color, #6366f1)' }}>
              <TruckIcon size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: '#ffffff' }}>
                {mode === 'create' ? 'Register New Fleet Truck' : 'Edit Vehicle Parameters'}
              </h2>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary, #9ca3af)' }}>
                {mode === 'create' ? 'Add a new delivery vehicle to the fleet catalog' : `Update parameters for truck #${truck?.id}`}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            style={{ background: 'none', border: 'none', color: 'var(--text-secondary, #9ca3af)', cursor: 'pointer', padding: '4px', borderRadius: '6px' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Global Server Error Alert Banner */}
        {serverGeneralError && (
          <div style={{
            display: 'flex', alignItems: 'flex-start', gap: '0.75rem', padding: '0.85rem 1rem',
            marginBottom: '1.25rem', borderRadius: '10px', background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', fontSize: '0.875rem'
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Failed to save vehicle:</strong> {serverGeneralError}
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>

            {/* Registration Number */}
            <div>
              <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                Registration Number <span style={{ color: 'var(--danger, #ef4444)' }}>*</span>
              </label>
              <input
                type="text"
                name="registrationNumber"
                value={formData.registrationNumber}
                onChange={handleChange}
                onBlur={handleBlur}
                disabled={isSubmitting}
                placeholder="e.g. TRK-8899"
                style={{
                  width: '100%', padding: '10px 14px', borderRadius: '10px',
                  border: `1px solid ${errors.registrationNumber ? 'var(--danger, #ef4444)' : 'var(--border-color, rgba(255,255,255,0.15))'}`,
                  background: 'rgba(255,255,255,0.03)', color: '#ffffff', fontSize: '0.9rem', outline: 'none'
                }}
              />
              {errors.registrationNumber && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger, #ef4444)', fontSize: '0.78rem', marginTop: '5px' }}>
                  <AlertCircle size={13} />
                  <span>{errors.registrationNumber}</span>
                </div>
              )}
            </div>

            {/* Make & Model */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Make <span style={{ color: 'var(--danger, #ef4444)' }}>*</span>
                </label>
                <input
                  type="text"
                  name="make"
                  value={formData.make}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  placeholder="e.g. Volvo"
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: `1px solid ${errors.make ? 'var(--danger, #ef4444)' : 'var(--border-color, rgba(255,255,255,0.15))'}`,
                    background: 'rgba(255,255,255,0.03)', color: '#ffffff', fontSize: '0.9rem', outline: 'none'
                  }}
                />
                {errors.make && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger, #ef4444)', fontSize: '0.78rem', marginTop: '5px' }}>
                    <AlertCircle size={13} />
                    <span>{errors.make}</span>
                  </div>
                )}
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Model <span style={{ color: 'var(--danger, #ef4444)' }}>*</span>
                </label>
                <input
                  type="text"
                  name="model"
                  value={formData.model}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  placeholder="e.g. FH16"
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: `1px solid ${errors.model ? 'var(--danger, #ef4444)' : 'var(--border-color, rgba(255,255,255,0.15))'}`,
                    background: 'rgba(255,255,255,0.03)', color: '#ffffff', fontSize: '0.9rem', outline: 'none'
                  }}
                />
                {errors.model && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger, #ef4444)', fontSize: '0.78rem', marginTop: '5px' }}>
                    <AlertCircle size={13} />
                    <span>{errors.model}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Capacity & Fuel Type */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Capacity (Tons)
                </label>
                <input
                  type="number"
                  name="capacity"
                  step="0.1"
                  min="0.1"
                  value={formData.capacity}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  placeholder="e.g. 25.0"
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: `1px solid ${errors.capacity ? 'var(--danger, #ef4444)' : 'var(--border-color, rgba(255,255,255,0.15))'}`,
                    background: 'rgba(255,255,255,0.03)', color: '#ffffff', fontSize: '0.9rem', outline: 'none'
                  }}
                />
                {errors.capacity && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger, #ef4444)', fontSize: '0.78rem', marginTop: '5px' }}>
                    <AlertCircle size={13} />
                    <span>{errors.capacity}</span>
                  </div>
                )}
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Fuel Type
                </label>
                <select
                  name="fuelType"
                  value={formData.fuelType}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: '1px solid var(--border-color, rgba(255,255,255,0.15))',
                    background: 'var(--bg-card, #1f2937)', color: '#ffffff', fontSize: '0.9rem', outline: 'none', cursor: 'pointer'
                  }}
                >
                  <option value="DIESEL">Diesel</option>
                  <option value="PETROL">Petrol</option>
                  <option value="ELECTRIC">Electric</option>
                  <option value="HYBRID">Hybrid</option>
                </select>
              </div>
            </div>

            {/* Assigned Driver & Last Service Date */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Assigned Driver
                </label>
                <select
                  name="driverEmployeeId"
                  value={formData.driverEmployeeId}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: '1px solid var(--border-color, rgba(255,255,255,0.15))',
                    background: 'var(--bg-card, #1f2937)', color: '#ffffff', fontSize: '0.9rem', outline: 'none', cursor: 'pointer'
                  }}
                >
                  <option value="">Unassigned</option>
                  {allDrivers.map((d) => (
                    <option key={d.id} value={d.id}>{d.name} ({d.employeeCode})</option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #9ca3af)', display: 'block', marginBottom: '6px' }}>
                  Last Service Date
                </label>
                <input
                  type="date"
                  name="lastServiceDate"
                  value={formData.lastServiceDate}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isSubmitting}
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: '10px',
                    border: '1px solid var(--border-color, rgba(255,255,255,0.15))',
                    background: 'rgba(255,255,255,0.03)', color: '#ffffff', fontSize: '0.9rem', outline: 'none'
                  }}
                />
              </div>
            </div>

          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem', borderTop: '1px solid var(--border-color, rgba(255,255,255,0.1))', paddingTop: '1.25rem' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="btn btn-secondary"
              style={{ padding: '8px 16px', borderRadius: '10px' }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn btn-primary"
              style={{ padding: '8px 20px', borderRadius: '10px', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              {isSubmitting ? (
                <>
                  <span style={{ display: 'inline-block', width: '14px', height: '14px', border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
                  Saving...
                </>
              ) : (
                <>
                  <Save size={16} />
                  {mode === 'create' ? 'Register Truck' : 'Update Vehicle'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
