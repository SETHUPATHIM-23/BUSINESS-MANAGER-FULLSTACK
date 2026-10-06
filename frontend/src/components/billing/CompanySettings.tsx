import React, { useState, useEffect } from 'react';
import { db } from './db';
import { COMPANY_DETAILS } from './types';
import { Building2, Landmark, Truck, Save, Plus, Trash2, ChevronDown } from 'lucide-react';
import { toastEvents } from '../../utils/toast';
import { api } from '../../utils/api';

export type CompanyVehicle = {
  id?: string;
  name: string;
  vehicleNo: string;
};

export type CompanyDetails = typeof COMPANY_DETAILS;

export const getStoredCompanyDetails = (): CompanyDetails => {
  try {
    const saved = localStorage.getItem('senthur_company_details');
    if (saved) {
      const parsed = JSON.parse(saved);
      return { ...COMPANY_DETAILS, ...parsed };
    }
  } catch (err) {
    console.error('Error reading company settings from localStorage', err);
  }
  return COMPANY_DETAILS;
};

export const fetchCompanySettingsFromBackend = async (): Promise<CompanyDetails> => {
  const local = getStoredCompanyDetails();
  try {
    const res = await api.get('/api/settings/company');
    const data = res.data;

    let dbVehicles: CompanyVehicle[] = [];
    if (data.vehiclesJson) {
      try {
        dbVehicles = JSON.parse(data.vehiclesJson);
      } catch (e) {
        console.error('Error parsing vehiclesJson from database', e);
      }
    }

    // Also fetch fleet vehicles from Fleet Management /api/trucks
    try {
      const truckRes = await api.get<{ content: any[] }>('/api/trucks?size=1000');
      const fleet = truckRes.data?.content || [];
      fleet.forEach((t: any) => {
        if (t.registrationNumber && !dbVehicles.some(v => v.vehicleNo.toUpperCase() === t.registrationNumber.toUpperCase())) {
          dbVehicles.push({
            id: `truck-${t.id}`,
            name: `${t.make || ''} ${t.model || ''}`.trim() || t.registrationNumber,
            vehicleNo: t.registrationNumber.toUpperCase()
          });
        }
      });
    } catch (e) {}

    // Also fetch tax rates from /api/lookups/tax-rates
    let dbTaxRates: any[] = [];
    try {
      const taxRes = await api.get('/api/lookups/tax-rates');
      dbTaxRates = (taxRes.data || []).map((t: any) => {
        const rate = Number(t.rate) || 0;
        const isIgst = Boolean(t.isIgst || (t.name && t.name.toUpperCase().includes('IGST')) || (t.label && t.label.toUpperCase().includes('IGST')));
        let components: Array<{ name: string; rate: number }> = [];
        if (t.componentsJson) {
          try {
            const parsed = JSON.parse(t.componentsJson);
            if (Array.isArray(parsed) && parsed.length > 0) {
              components = parsed.map((c: any) => ({ name: String(c.name), rate: Number(c.rate) || 0 }));
            }
          } catch (e) {}
        }
        const cgst = t.cgstRate != null ? Number(t.cgstRate) : 0;
        const sgst = t.sgstRate != null ? Number(t.sgstRate) : 0;
        const igst = t.igstRate != null ? Number(t.igstRate) : 0;
        let cgstRate = cgst;
        let sgstRate = sgst;
        let igstRate = igst;
        if (components.length === 0) {
          if (cgst > 0) components.push({ name: 'CGST', rate: cgst });
          if (sgst > 0) components.push({ name: 'SGST', rate: sgst });
          if (igst > 0) components.push({ name: 'IGST', rate: igst });
          if (components.length === 0 && rate > 0) {
            if (isIgst) {
              components.push({ name: 'IGST', rate: rate });
              igstRate = rate;
            } else {
              components.push({ name: 'CGST', rate: rate / 2 });
              components.push({ name: 'SGST', rate: rate / 2 });
              cgstRate = rate / 2;
              sgstRate = rate / 2;
            }
          }
        }
        return {
          id: String(t.id),
          label: t.name || t.label || `GST ${rate}%`,
          rate: rate,
          cgstRate,
          sgstRate,
          igstRate,
          components
        };
      });
    } catch (e) {}

    let bankInfo: any = {};
    if (data.bankDetailsJson) {
      try {
        bankInfo = JSON.parse(data.bankDetailsJson);
      } catch (e) {}
    }

    const merged: CompanyDetails = {
      ...local,
      name: data.companyName || local.name,
      address: data.address || local.address,
      mobile: data.mobile || local.mobile,
      gstin: data.gstin || local.gstin,
      bankName: bankInfo.bankName || local.bankName,
      accountNo: bankInfo.accountNo || bankInfo.accountNumber || local.accountNo,
      ifscCode: bankInfo.ifsc || local.ifscCode,
      branch: bankInfo.branch || local.branch,
      vehicles: dbVehicles.length > 0 ? dbVehicles : (local.vehicles || []),
      taxRates: dbTaxRates.length > 0 ? dbTaxRates : (local.taxRates || []),
      ...(data.legalName ? { legalName: data.legalName } : {}),
      ...(data.city ? { city: data.city } : {}),
      ...(data.state ? { state: data.state } : {}),
      ...(data.stateCode ? { stateCode: data.stateCode } : {}),
      ...(data.pincode ? { pincode: data.pincode } : {}),
      ...(data.country ? { country: data.country } : {}),
      ...(data.telephone ? { phone: data.telephone } : {}),
      ...(data.email ? { email: data.email } : {}),
      ...(data.website ? { website: data.website } : {}),
      ...(data.pan ? { pan: data.pan } : {})
    };

    localStorage.setItem('senthur_company_details', JSON.stringify(merged));
    return merged;
  } catch (err) {
    console.error('Failed to fetch company settings from backend', err);
    return local;
  }
};

interface Props {
  currentSettings: CompanyDetails;
  onSettingsChanged: (newSettings: CompanyDetails) => void;
}

export const CompanySettings: React.FC<Props> = ({ currentSettings, onSettingsChanged }) => {
  const [settings, setSettings] = useState(currentSettings);
  const [isExpanded, setIsExpanded] = useState(true);

  const [newVehName, setNewVehName] = useState('');
  const [newVehNo, setNewVehNo] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchCompanySettingsFromBackend().then(loaded => {
      setSettings(loaded);
      onSettingsChanged(loaded);
    });
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload: any = {
        companyName: settings.name,
        legalName: (settings as any).legalName || settings.name,
        address: settings.address,
        city: (settings as any).city || 'Bhavani',
        state: (settings as any).state || 'Tamil Nadu',
        stateCode: (settings as any).stateCode || '33',
        pincode: (settings as any).pincode || '638301',
        country: (settings as any).country || 'India',
        mobile: settings.mobile,
        telephone: (settings as any).phone || '',
        email: (settings as any).email || '',
        website: (settings as any).website || '',
        gstin: settings.gstin,
        pan: (settings as any).pan || '',
        vehiclesJson: JSON.stringify(settings.vehicles || []),
        bankDetailsJson: JSON.stringify({
          bankName: settings.bankName,
          accountNo: settings.accountNo,
          ifsc: settings.ifscCode,
          branch: settings.branch
        })
      };

      await api.put('/api/settings/company', payload);
      await db.saveSettings(settings);
      localStorage.setItem('senthur_company_details', JSON.stringify(settings));
      onSettingsChanged(settings);
      toastEvents.success('Company profile & vehicle data saved permanently to database');
    } catch (err) {
      toastEvents.error('Failed to save company settings to database');
    } finally {
      setSaving(false);
    }
  };

  const updateField = (field: keyof CompanyDetails, value: any) => {
    setSettings(prev => ({ ...prev, [field]: value }));
  };

  const handleAddVehicle = () => {
    if (!newVehNo.trim()) {
      toastEvents.error('Please enter a vehicle number');
      return;
    }
    const vehicleItem: CompanyVehicle = {
      id: Date.now().toString(),
      name: newVehName.trim() || newVehNo.trim().toUpperCase(),
      vehicleNo: newVehNo.trim().toUpperCase()
    };
    const currentVehicles = settings.vehicles || [];
    updateField('vehicles', [...currentVehicles, vehicleItem]);
    setNewVehName('');
    setNewVehNo('');
    toastEvents.success(`Vehicle ${vehicleItem.vehicleNo} added`);
  };

  const handleRemoveVehicle = (index: number) => {
    const currentVehicles = settings.vehicles || [];
    const updated = currentVehicles.filter((_, i) => i !== index);
    updateField('vehicles', updated);
  };

  return (
    <div className="glass-panel" style={{ borderRadius: 'var(--border-radius-lg)', overflow: 'hidden' }}>
      {/* Header Banner */}
      <div 
        onClick={() => setIsExpanded(!isExpanded)}
        style={{
          padding: '1.25rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          borderBottom: isExpanded ? '1px solid var(--border-color)' : 'none',
          backgroundColor: 'rgba(37, 99, 235, 0.04)',
          transition: 'all 0.15s ease'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ padding: '0.6rem', borderRadius: 'var(--border-radius-sm)', backgroundColor: 'rgba(37, 99, 235, 0.12)', color: 'var(--accent-primary)' }}>
            <Building2 size={22} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
              Company Profile, Bank & Transport Settings
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
              Master GSTIN, legal address, bank account parameters, and quick vehicle presets.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, padding: '3px 10px', borderRadius: '12px', backgroundColor: 'var(--success-bg)', color: 'var(--success-text)' }}>
            GSTIN: {settings.gstin || 'Configured'}
          </span>
          <ChevronDown size={20} style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease', color: 'var(--text-muted)' }} />
        </div>
      </div>

      {/* Expanded Content Form */}
      {isExpanded && (
        <form onSubmit={handleSave} style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* General Information Section */}
          <div>
            <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--accent-primary)', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Building2 size={16} /> General Organization Info
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Company Name *</label>
                <input
                  type="text"
                  value={settings.name}
                  onChange={e => updateField('name', e.target.value)}
                  className="form-control"
                  required
                />
              </div>

              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">GSTIN / Tax ID *</label>
                <input
                  type="text"
                  value={settings.gstin}
                  onChange={e => updateField('gstin', e.target.value)}
                  className="form-control"
                  style={{ fontFamily: 'monospace', textTransform: 'uppercase' }}
                  required
                />
              </div>

              <div className="form-group" style={{ gridColumn: '1 / -1', margin: 0 }}>
                <label className="form-label">Registered Office Address *</label>
                <textarea
                  rows={2}
                  value={settings.address}
                  onChange={e => updateField('address', e.target.value)}
                  className="form-control"
                  required
                />
              </div>

              <div className="form-group" style={{ gridColumn: '1 / -1', margin: 0 }}>
                <label className="form-label">Official Mobile / Support Numbers</label>
                <input
                  type="text"
                  value={settings.mobile}
                  onChange={e => updateField('mobile', e.target.value)}
                  className="form-control"
                  placeholder="e.g. 9842737137, 6381664652"
                />
              </div>
            </div>
          </div>

          <hr style={{ borderColor: 'var(--border-color)', margin: 0 }} />

          {/* Bank Account Section */}
          <div>
            <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--accent-primary)', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Landmark size={16} /> Bank Account Details (Printed on Invoices)
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Bank Name</label>
                <input
                  type="text"
                  value={settings.bankName}
                  onChange={e => updateField('bankName', e.target.value)}
                  className="form-control"
                  placeholder="e.g. State Bank of India"
                />
              </div>

              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Account Number</label>
                <input
                  type="text"
                  value={settings.accountNo}
                  onChange={e => updateField('accountNo', e.target.value)}
                  className="form-control"
                  style={{ fontFamily: 'monospace' }}
                  placeholder="e.g. 30891234567"
                />
              </div>

              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">IFSC Code</label>
                <input
                  type="text"
                  value={settings.ifscCode}
                  onChange={e => updateField('ifscCode', e.target.value)}
                  className="form-control"
                  style={{ fontFamily: 'monospace', textTransform: 'uppercase' }}
                  placeholder="e.g. SBIN0001234"
                />
              </div>

              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Branch Location</label>
                <input
                  type="text"
                  value={settings.branch}
                  onChange={e => updateField('branch', e.target.value)}
                  className="form-control"
                  placeholder="e.g. Bhavani Main Branch"
                />
              </div>
            </div>
          </div>

          <hr style={{ borderColor: 'var(--border-color)', margin: 0 }} />

          {/* Company Vehicles Section */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--accent-primary)', margin: 0, display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Truck size={16} /> Saved Transport Vehicles
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '2px 0 0' }}>
                  Quick vehicle presets available in billing and delivery dispatches.
                </p>
              </div>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                {(settings.vehicles || []).length} vehicle(s) saved
              </span>
            </div>

            {/* Vehicles List */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '0.75rem', marginBottom: '1rem' }}>
              {(!settings.vehicles || settings.vehicles.length === 0) ? (
                <div style={{ gridColumn: '1 / -1', padding: '1rem', textAlign: 'center', border: '1px dashed var(--border-color)', borderRadius: 'var(--border-radius-sm)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  No transport vehicles added yet. Use the form below to register a vehicle.
                </div>
              ) : (
                settings.vehicles.map((v, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '0.75rem',
                      borderRadius: 'var(--border-radius-sm)',
                      backgroundColor: 'var(--bg-tertiary)',
                      border: '1px solid var(--border-color)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between'
                    }}
                  >
                    <div>
                      <div style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                        {v.vehicleNo}
                      </div>
                      {v.name && v.name !== v.vehicleNo && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                          {v.name}
                        </div>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => handleRemoveVehicle(idx)}
                      className="btn btn-outline btn-sm"
                      style={{ padding: '4px 6px', color: 'var(--danger)' }}
                      title="Remove vehicle"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))
              )}
            </div>

            {/* Add Vehicle Form */}
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap', padding: '1rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--border-radius-sm)', border: '1px solid var(--border-color)' }}>
              <div style={{ flex: 1, minWidth: '180px' }}>
                <label className="form-label" style={{ fontSize: '0.75rem' }}>Vehicle Description / Label</label>
                <input
                  type="text"
                  placeholder="e.g. Eicher 14-Ft Truck"
                  value={newVehName}
                  onChange={e => setNewVehName(e.target.value)}
                  className="form-control"
                  style={{ height: '34px', fontSize: '0.825rem' }}
                />
              </div>

              <div style={{ flex: 1, minWidth: '180px' }}>
                <label className="form-label" style={{ fontSize: '0.75rem' }}>Vehicle Reg Number *</label>
                <input
                  type="text"
                  placeholder="e.g. TN 33 AB 1234"
                  value={newVehNo}
                  onChange={e => setNewVehNo(e.target.value)}
                  className="form-control"
                  style={{ height: '34px', fontSize: '0.825rem', fontFamily: 'monospace', textTransform: 'uppercase' }}
                />
              </div>

              <button
                type="button"
                onClick={handleAddVehicle}
                className="btn btn-secondary btn-sm"
                style={{ height: '34px', padding: '0 14px' }}
              >
                <Plus size={14} /> Add Vehicle
              </button>
            </div>
          </div>

          {/* Submit Settings Button */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '0.5rem' }}>
            <button type="submit" className="btn btn-primary btn-lg" disabled={saving}>
              <Save size={18} /> {saving ? 'Saving Settings…' : 'Save Company Profile'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default CompanySettings;
