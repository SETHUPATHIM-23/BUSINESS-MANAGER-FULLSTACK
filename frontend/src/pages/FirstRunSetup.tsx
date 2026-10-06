import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../utils/api';

export const FirstRunSetup: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState<number>(1);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  // Form State
  const [companyName, setCompanyName] = useState<string>('');
  const [legalName] = useState<string>('');
  const [address, setAddress] = useState<string>('');
  const [city, setCity] = useState<string>('');
  const [state, setState] = useState<string>('');
  const [stateCode] = useState<string>('');
  const [pincode, setPincode] = useState<string>('');
  const [country] = useState<string>('India');
  const [mobile, setMobile] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [gstin, setGstin] = useState<string>('');
  const [pan, setPan] = useState<string>('');

  // Admin Account State
  const [adminUsername, setAdminUsername] = useState<string>('admin');
  const [adminPassword, setAdminPassword] = useState<string>('');
  const [confirmPassword, setConfirmPassword] = useState<string>('');

  // Business Settings State
  const [invoicePrefix, setInvoicePrefix] = useState<string>('INV-');
  const [invoiceNextNumber, setInvoiceNextNumber] = useState<number>(1);
  const [currencySymbol, setCurrencySymbol] = useState<string>('₹');
  const [currencyCode, setCurrencyCode] = useState<string>('INR');
  const [invoiceTerms, setInvoiceTerms] = useState<string>('Payment due within 30 days of invoice date.');
  const [invoiceFooter] = useState<string>('Thank you for your business!');

  const handleCompleteSetup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (adminPassword !== confirmPassword) {
      setError('Admin passwords do not match');
      return;
    }
    if (adminPassword.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await api.post('/api/v1/setup/initialize', {
        adminUsername,
        adminPassword,
        companyName,
        legalName: legalName || companyName,
        address,
        city,
        state,
        stateCode,
        pincode,
        country,
        mobile,
        email,
        gstin,
        pan,
        invoicePrefix,
        invoiceNextNumber,
        currencySymbol,
        currencyCode,
        invoiceTerms,
        invoiceFooter,
      });

      // Navigate to login after initialization completes
      navigate('/login', { state: { message: 'System setup completed successfully! Please log in.' } });
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to complete initial system setup. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#0f172a', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem', fontFamily: 'sans-serif' }}>
      <div style={{ width: '100%', maxWidth: '640px', backgroundColor: '#1e293b', borderRadius: '1rem', border: '1px solid #334155', padding: '2rem', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)' }}>
        
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '3.5rem', height: '3.5rem', borderRadius: '1rem', backgroundColor: '#3b82f6', color: '#ffffff', fontWeight: 'bold', fontSize: '1.5rem', marginBottom: '1rem' }}>
            SMC
          </div>
          <h1 style={{ color: '#f8fafc', fontSize: '1.75rem', fontWeight: 700, margin: 0 }}>SMC Management Enterprise</h1>
          <p style={{ color: '#94a3b8', fontSize: '0.875rem', marginTop: '0.5rem' }}>First-Time Production System Initialization</p>
        </div>

        {/* Step Indicator */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem', borderBottom: '1px solid #334155', paddingBottom: '1rem' }}>
          {[
            { num: 1, label: '1. Company Details' },
            { num: 2, label: '2. Admin Security' },
            { num: 3, label: '3. Business Settings' },
          ].map((s) => (
            <div key={s.num} style={{ color: step === s.num ? '#60a5fa' : step > s.num ? '#34d399' : '#64748b', fontWeight: step === s.num ? 600 : 400, fontSize: '0.875rem' }}>
              {s.label}
            </div>
          ))}
        </div>

        {error && (
          <div style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid #ef4444', color: '#fca5a5', padding: '0.75rem 1rem', borderRadius: '0.5rem', marginBottom: '1.5rem', fontSize: '0.875rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleCompleteSetup}>
          {/* STEP 1: Company Profile */}
          {step === 1 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem', fontWeight: 500 }}>Company Name *</label>
                <input
                  type="text"
                  required
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="e.g. Acme Enterprises Ltd"
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>GSTIN / Tax ID</label>
                  <input
                    type="text"
                    value={gstin}
                    onChange={(e) => setGstin(e.target.value)}
                    placeholder="e.g. 33AAAAA0000A1Z5"
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>PAN</label>
                  <input
                    type="text"
                    value={pan}
                    onChange={(e) => setPan(e.target.value)}
                    placeholder="e.g. AAAAA0000A"
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Address</label>
                <textarea
                  rows={2}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Street address, building, suite"
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 140px), 1fr))', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>City</label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>State</label>
                  <input
                    type="text"
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Pincode</label>
                  <input
                    type="text"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Mobile / Phone</label>
                  <input
                    type="text"
                    value={mobile}
                    onChange={(e) => setMobile(e.target.value)}
                    placeholder="+91 9876543210"
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Email</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="contact@company.com"
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (!companyName.trim()) {
                    setError('Please provide your Company Name');
                    return;
                  }
                  setError('');
                  setStep(2);
                }}
                style={{ marginTop: '1rem', width: '100%', padding: '0.75rem', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '0.5rem', fontWeight: 600, cursor: 'pointer' }}
              >
                Next: Administrator Account →
              </button>
            </div>
          )}

          {/* STEP 2: Administrator Account */}
          {step === 2 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem', fontWeight: 500 }}>Admin Username *</label>
                <input
                  type="text"
                  required
                  value={adminUsername}
                  onChange={(e) => setAdminUsername(e.target.value)}
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem', fontWeight: 500 }}>Admin Password *</label>
                <input
                  type="password"
                  required
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem', fontWeight: 500 }}>Confirm Password *</label>
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  style={{ width: '50%', padding: '0.75rem', backgroundColor: '#334155', color: '#f8fafc', border: 'none', borderRadius: '0.5rem', fontWeight: 500, cursor: 'pointer' }}
                >
                  ← Back
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (!adminUsername.trim() || !adminPassword) {
                      setError('Admin username and password are required');
                      return;
                    }
                    if (adminPassword !== confirmPassword) {
                      setError('Passwords do not match');
                      return;
                    }
                    if (adminPassword.length < 8) {
                      setError('Password must be at least 8 characters');
                      return;
                    }
                    setError('');
                    setStep(3);
                  }}
                  style={{ width: '50%', padding: '0.75rem', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '0.5rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Next: Settings →
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: Business & Billing Defaults */}
          {step === 3 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Invoice Prefix</label>
                  <input
                    type="text"
                    value={invoicePrefix}
                    onChange={(e) => setInvoicePrefix(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Starting Invoice Number</label>
                  <input
                    type="number"
                    min={1}
                    value={invoiceNextNumber}
                    onChange={(e) => setInvoiceNextNumber(parseInt(e.target.value) || 1)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Currency Symbol</label>
                  <input
                    type="text"
                    value={currencySymbol}
                    onChange={(e) => setCurrencySymbol(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Currency Code</label>
                  <input
                    type="text"
                    value={currencyCode}
                    onChange={(e) => setCurrencyCode(e.target.value)}
                    style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', color: '#cbd5e1', fontSize: '0.875rem', marginBottom: '0.375rem' }}>Invoice Terms</label>
                <input
                  type="text"
                  value={invoiceTerms}
                  onChange={(e) => setInvoiceTerms(e.target.value)}
                  style={{ width: '100%', padding: '0.625rem 0.875rem', backgroundColor: '#0f172a', border: '1px solid #334155', borderRadius: '0.5rem', color: '#f8fafc', fontSize: '0.875rem' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  style={{ width: '40%', padding: '0.75rem', backgroundColor: '#334155', color: '#f8fafc', border: 'none', borderRadius: '0.5rem', fontWeight: 500, cursor: 'pointer' }}
                >
                  ← Back
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  style={{ width: '60%', padding: '0.75rem', backgroundColor: '#10b981', color: '#ffffff', border: 'none', borderRadius: '0.5rem', fontWeight: 700, cursor: submitting ? 'not-allowed' : 'pointer' }}
                >
                  {submitting ? 'Initializing Production System...' : 'Complete System Setup ✓'}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
