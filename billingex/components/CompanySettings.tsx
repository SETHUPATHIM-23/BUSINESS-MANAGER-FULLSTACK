
import React, { useState, useEffect } from 'react';
import { db } from '../db';
import { COMPANY_DETAILS, CompanyVehicle } from '../types';

interface Props {
  currentSettings: typeof COMPANY_DETAILS;
  onSettingsChanged: (newSettings: typeof COMPANY_DETAILS) => void;
}

const CompanySettings: React.FC<Props> = ({ currentSettings, onSettingsChanged }) => {
  const [settings, setSettings] = useState(currentSettings);
  const [isExpanded, setIsExpanded] = useState(false);

  const [newVehName, setNewVehName] = useState('');
  const [newVehNo, setNewVehNo] = useState('');

  useEffect(() => {
    if (currentSettings) {
      setSettings(currentSettings);
    }
  }, [currentSettings]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await db.saveSettings(settings);
      onSettingsChanged(settings);
      alert('Company details updated successfully');
      setIsExpanded(false);
    } catch (err) {
      alert('Failed to save settings');
    }
  };

  const updateField = (field: keyof typeof COMPANY_DETAILS, value: any) => {
    setSettings(prev => ({ ...prev, [field]: value }));
  };

  const handleAddVehicle = () => {
    if (!newVehNo.trim()) {
      alert('Please enter a vehicle number');
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
  };

  const handleRemoveVehicle = (index: number) => {
    const currentVehicles = settings.vehicles || [];
    const updated = currentVehicles.filter((_, i) => i !== index);
    updateField('vehicles', updated);
  };

  const inputClasses = "mt-1 block w-full rounded-md border-gray-300 bg-white text-gray-900 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 border p-2 text-sm";

  return (
    <div className="bg-white rounded-lg shadow-sm border mb-6 no-print overflow-hidden">
      <div 
        className="p-6 cursor-pointer flex justify-between items-center bg-indigo-50 hover:bg-indigo-100 transition"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <h2 className="text-xl font-bold flex items-center gap-2 text-indigo-900">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-10V4a1 1 0 011-1h2a1 1 0 011 1v3M6 7h1m-1 4h1m3 0h3m-3 4h3m-6 0h1" />
          </svg>
          Company Profile & Bank Details
        </h2>
        <svg xmlns="http://www.w3.org/2000/svg" className={`h-6 w-6 transform transition-transform ${isExpanded ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {isExpanded && (
        <form onSubmit={handleSave} className="p-6 border-t space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <h3 className="text-xs font-bold uppercase text-indigo-600 mb-2 border-b pb-1">General Info</h3>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Company Name</label>
              <input type="text" value={settings.name} onChange={e => updateField('name', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">GSTIN</label>
              <input type="text" value={settings.gstin} onChange={e => updateField('gstin', e.target.value)} className={inputClasses} />
            </div>
            <div className="md:col-span-2">
              <label className="block text-xs font-bold text-gray-700 uppercase">Address</label>
              <textarea rows={2} value={settings.address} onChange={e => updateField('address', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Mobile Numbers</label>
              <input type="text" value={settings.mobile} onChange={e => updateField('mobile', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Default GST Percentage (%)</label>
              <div className="flex flex-col sm:flex-row gap-2 mt-1">
                <input 
                  type="number" 
                  min="0" 
                  max="100" 
                  step="any" 
                  value={settings.gstPercentage !== undefined ? settings.gstPercentage : 18} 
                  onChange={e => {
                    const val = parseFloat(e.target.value);
                    updateField('gstPercentage', isNaN(val) ? 0 : val);
                  }} 
                  className={`${inputClasses} mt-0 flex-grow`} 
                />
                <div className="flex gap-1 items-center">
                  {[0, 5, 12, 18, 28].map(pct => (
                    <button
                      key={pct}
                      type="button"
                      onClick={() => updateField('gstPercentage', pct)}
                      className={`px-2 py-2 text-xs font-bold rounded border transition-colors ${settings.gstPercentage === pct ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200'}`}
                    >
                      {pct}%
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t">
            <div className="md:col-span-2">
              <h3 className="text-xs font-bold uppercase text-indigo-600 mb-2 border-b pb-1">Bank Account Info</h3>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Bank Name</label>
              <input type="text" value={settings.bankName} onChange={e => updateField('bankName', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Account Number</label>
              <input type="text" value={settings.accountNo} onChange={e => updateField('accountNo', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">IFSC Code</label>
              <input type="text" value={settings.ifscCode} onChange={e => updateField('ifscCode', e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Branch</label>
              <input type="text" value={settings.branch} onChange={e => updateField('branch', e.target.value)} className={inputClasses} />
            </div>
          </div>

          <div className="pt-4 border-t">
            <div className="mb-3">
              <h3 className="text-xs font-bold uppercase text-indigo-600 border-b pb-1">Saved Company Vehicles (For Quick Invoice Selection)</h3>
              <p className="text-xs text-gray-500 mt-1">Store vehicle numbers with names/descriptions so you can select them directly when creating invoices.</p>
            </div>

            {/* List of current saved vehicles */}
            <div className="mb-4">
              {(!settings.vehicles || settings.vehicles.length === 0) ? (
                <p className="text-xs italic text-gray-400 bg-gray-50 p-2 rounded border border-dashed">No vehicles saved yet. Add one below.</p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                  {settings.vehicles.map((v, idx) => (
                    <div key={idx} className="flex items-center justify-between bg-indigo-50/60 border border-indigo-200 rounded p-2 text-xs">
                      <div className="truncate pr-2">
                        <div className="font-bold text-indigo-950 uppercase">{v.vehicleNo}</div>
                        {v.name && v.name !== v.vehicleNo && (
                          <div className="text-[11px] text-gray-600 truncate">{v.name}</div>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => handleRemoveVehicle(idx)}
                        className="text-red-600 hover:text-red-800 font-bold p-1 hover:bg-red-50 rounded transition"
                        title="Delete vehicle"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Form to add vehicle */}
            <div className="bg-gray-50 p-3 rounded-md border flex flex-col sm:flex-row gap-2 items-end">
              <div className="flex-1 w-full">
                <label className="block text-[11px] font-bold text-gray-600 uppercase">Vehicle Name / Label (e.g. Eicher / Driver Suresh)</label>
                <input
                  type="text"
                  placeholder="e.g. Eicher Truck"
                  value={newVehName}
                  onChange={e => setNewVehName(e.target.value)}
                  className="mt-1 block w-full rounded border-gray-300 bg-white text-gray-900 border p-1.5 text-xs"
                />
              </div>
              <div className="flex-1 w-full">
                <label className="block text-[11px] font-bold text-gray-600 uppercase">Vehicle Number *</label>
                <input
                  type="text"
                  placeholder="e.g. TN 36 AB 1234"
                  value={newVehNo}
                  onChange={e => setNewVehNo(e.target.value)}
                  className="mt-1 block w-full rounded border-gray-300 bg-white font-bold text-gray-900 border p-1.5 text-xs uppercase"
                />
              </div>
              <button
                type="button"
                onClick={handleAddVehicle}
                className="w-full sm:w-auto bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 transition font-bold text-xs uppercase whitespace-nowrap"
              >
                + Add Vehicle
              </button>
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button type="submit" className="bg-indigo-600 text-white px-8 py-2 rounded hover:bg-indigo-700 transition font-bold shadow-sm uppercase text-xs tracking-widest">
              Save Profile
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default CompanySettings;
