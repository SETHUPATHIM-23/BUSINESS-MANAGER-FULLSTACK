
import React, { useState } from 'react';
import { db } from '../db';
import { Customer } from '../types';

interface Props {
  customers: Customer[];
  onCustomerChanged: () => void;
}

const CustomerManager: React.FC<Props> = ({ customers, onCustomerChanged }) => {
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [gstin, setGstin] = useState('');
  const [state, setState] = useState('Tamil Nadu');
  const [stateCode, setStateCode] = useState('33');
  const [isExpanded, setIsExpanded] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !address) return;

    try {
      await db.addCustomer({
        name,
        address,
        gstin,
        state,
        stateCode
      });

      setName('');
      setAddress('');
      setGstin('');
      onCustomerChanged();
      alert('Customer added successfully');
    } catch (error) {
      console.error(error);
      alert('Failed to add customer. Please check database permissions.');
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: number, customerName: string) => {
    // Prevent the click from bubbling up to any parents
    e.stopPropagation();
    
    if (window.confirm(`Are you sure you want to remove ${customerName}? This will not delete their existing invoices.`)) {
      try {
        await db.deleteCustomer(id);
        onCustomerChanged();
        alert('Customer removed successfully');
      } catch (error) {
        console.error('Delete failed:', error);
        alert('Could not remove customer. Please refresh and try again.');
      }
    }
  };

  const inputClasses = "mt-1 block w-full rounded-md border-gray-300 bg-white text-gray-900 shadow-sm focus:border-blue-500 focus:ring-blue-500 border p-2";

  return (
    <div className="bg-white rounded-lg shadow-sm border mb-6 no-print overflow-hidden">
      <div 
        className="p-6 cursor-pointer flex justify-between items-center bg-gray-50 hover:bg-gray-100 transition"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          Manage Customers ({customers.length})
        </h2>
        <svg xmlns="http://www.w3.org/2000/svg" className={`h-6 w-6 transform transition-transform ${isExpanded ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {isExpanded && (
        <div className="p-6 border-t">
          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8 bg-blue-50/30 p-4 rounded-lg border border-blue-100">
            <h3 className="lg:col-span-3 text-sm font-bold uppercase text-blue-800 mb-2">Add New Customer</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700">Customer Name *</label>
              <input type="text" value={name} onChange={(e) => setName(e.target.value)} required className={inputClasses} />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700">Address *</label>
              <textarea 
                value={address} 
                onChange={(e) => setAddress(e.target.value)} 
                required 
                rows={2}
                className={inputClasses} 
                placeholder="Full billing address..."
              ></textarea>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">GSTIN (Optional)</label>
              <input type="text" value={gstin} onChange={(e) => setGstin(e.target.value)} className={inputClasses} placeholder="33..." />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">State</label>
              <input type="text" value={state} onChange={(e) => setState(e.target.value)} className={inputClasses} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">State Code</label>
              <input type="text" value={stateCode} onChange={(e) => setStateCode(e.target.value)} className={inputClasses} />
            </div>
            <div className="lg:col-span-3 flex justify-end">
              <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition font-semibold shadow-sm">Add Customer</button>
            </div>
          </form>

          <div className="overflow-x-auto">
            <h3 className="text-sm font-bold uppercase text-gray-800 mb-4">Saved Customers List</h3>
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b text-xs font-bold uppercase text-gray-600">
                  <th className="p-3">Name</th>
                  <th className="p-3">GSTIN</th>
                  <th className="p-3">State</th>
                  <th className="p-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y text-sm">
                {customers.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50 transition">
                    <td className="p-3 font-medium">{c.name}</td>
                    <td className="p-3 font-mono text-gray-500">{c.gstin || 'N/A'}</td>
                    <td className="p-3">{c.state} ({c.stateCode})</td>
                    <td className="p-3 text-right">
                      <button 
                        type="button"
                        onClick={(e) => c.id !== undefined && handleDelete(e, c.id, c.name)}
                        className="text-red-500 hover:text-red-700 font-bold flex items-center gap-1 ml-auto transition-colors px-2 py-1 rounded hover:bg-red-50"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
                {customers.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-gray-400 italic">No customers saved yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default CustomerManager;
