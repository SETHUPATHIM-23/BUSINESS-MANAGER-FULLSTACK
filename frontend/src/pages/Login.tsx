import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, User, AlertCircle, RefreshCw, Eye, EyeOff } from 'lucide-react';
import { getStoredCompanyDetails, fetchCompanySettingsFromBackend, type CompanyDetails } from '../components/billing/CompanySettings';

export const Login: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const [company, setCompany] = useState<CompanyDetails>(() => getStoredCompanyDetails());

  useEffect(() => {
    fetchCompanySettingsFromBackend().then(loaded => setCompany(loaded));
  }, []);

  /* Auth redirect */
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const isExpired = searchParams.get('expired') === 'true';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) { setError('Please fill in all fields'); return; }
    setError(null);
    setIsLoading(true);
    try {
      await login(username, password);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const axErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axErr.response?.status === 401) setError('Invalid username or password');
      else if (axErr.response?.data?.message) setError(axErr.response.data.message);
      else setError('Connection failed. Please ensure the backend is running.');
    } finally {
      setIsLoading(false);
    }
  };

  const ETHNO: React.CSSProperties = { fontFamily: "'Ethnocentric', sans-serif" };

  return (
    <div style={{
      minHeight: '100vh',
      width: '100vw',
      display: 'flex',
      background: '#01095a',
      position: 'relative',
      overflow: 'hidden',
    }}>
      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes pulseGlow {
          0%, 100% { box-shadow: 0 0 25px rgba(37, 99, 235, 0.3); }
          50%       { box-shadow: 0 0 50px rgba(56, 189, 248, 0.45); }
        }
        .ethno { font-family: 'Ethnocentric', sans-serif !important; }
        .glass-input {
          width: 100%;
          padding: 13px 14px 13px 44px;
          border-radius: 12px;
          border: 1px solid rgba(255, 255, 255, 0.2);
          background: rgba(255, 255, 255, 0.08);
          color: #ffffff;
          font-size: 0.88rem;
          font-family: 'Ethnocentric', sans-serif;
          outline: none;
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          transition: border-color 0.25s, background 0.25s, box-shadow 0.25s;
          box-sizing: border-box;
        }
        .glass-input::placeholder { color: rgba(255, 255, 255, 0.4); }
        .glass-input:focus {
          border-color: #38bdf8;
          background: rgba(56, 189, 248, 0.12);
          box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.25);
        }
        .btn-signin {
          width: 100%;
          height: 50px;
          border-radius: 12px;
          border: none;
          background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 50%, #1e40af 100%);
          color: #ffffff;
          font-family: 'Ethnocentric', sans-serif;
          font-size: 0.88rem;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
          animation: pulseGlow 3.5s ease-in-out infinite;
        }
        .btn-signin:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 10px 35px rgba(37, 99, 235, 0.55);
        }
        .btn-signin:active:not(:disabled){ transform: translateY(0); }
        .btn-signin:disabled { opacity: 0.6; cursor: not-allowed; }

        @media (max-width: 768px) {
          .login-bg-left { display: none !important; }
          .login-bg-gradient { background: #01095a !important; }
          .login-right-panel {
            width: 100% !important;
            margin: 0 !important;
            padding: 1.5rem 1rem !important;
          }
          .login-glass-card {
            padding: 1.75rem 1.25rem !important;
            border-radius: 16px !important;
          }
        }
      `}</style>

      {/* ════════════════════════════════════
          BACKGROUND — Image container & Smooth Slow Gradient
      ════════════════════════════════════ */}
      <div className="login-bg-left" style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '65%',
        height: '100%',
        backgroundColor: '#ffffff',
        zIndex: 0
      }}>
        <img
          src="/senthur_logo.jpg"
          alt="Senthur Chemical Logo"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            objectPosition: 'left center',
            padding: '2rem 10% 2rem 3rem', // Add padding to avoid the gradient edge
            boxSizing: 'border-box'
          }}
        />
      </div>

      <div className="login-bg-gradient" style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        background: 'linear-gradient(to right, rgba(1, 9, 90, 0) 0%, rgba(1, 9, 90, 0) 30%, rgba(1, 9, 90, 1) 65%)',
        zIndex: 1,
        pointerEvents: 'none',
      }} />

      {/* ════════════════════════════════════
          RIGHT PANEL — Frosted Glass Login Box
      ════════════════════════════════════ */}
      <div className="login-right-panel" style={{
        position: 'relative',
        marginLeft: 'auto',
        width: '50%',
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem',
        zIndex: 2,
        boxSizing: 'border-box',
      }}>
        {/* Subtle Ambient Cobalt Radial Glow */}
        <div style={{
          position: 'absolute', bottom: '-80px', right: '-80px',
          width: '350px', height: '350px', borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(56, 189, 248, 0.12) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />

        <div className="login-glass-card" style={{ 
          width: '100%', 
          maxWidth: '420px', 
          position: 'relative', 
          zIndex: 2,
          background: 'rgba(255, 255, 255, 0.05)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          borderRadius: '24px',
          padding: '2.5rem',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
        }}>

          {/* Header */}
          <div style={{ marginBottom: '2.5rem' }}>
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              padding: '5px 14px', borderRadius: '20px',
              background: 'rgba(56, 189, 248, 0.12)', border: '1px solid rgba(56, 189, 248, 0.35)',
              marginBottom: '1.2rem',
            }}>
              <div style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#38bdf8' }} />
              <span style={{ ...ETHNO, fontSize: '0.65rem', color: '#38bdf8', letterSpacing: '0.1em' }}>SECURE ACCESS</span>
            </div>

            <h2 style={{
              ...ETHNO,
              fontSize: '1.75rem',
              color: '#ffffff',
              fontWeight: 700,
              lineHeight: 1.2,
              marginBottom: '0.5rem',
              letterSpacing: '0.04em',
            }}>
              Welcome Back
            </h2>
            <p style={{
              ...ETHNO,
              fontSize: '0.72rem',
              color: 'rgba(255, 255, 255, 0.65)',
              letterSpacing: '0.06em',
              lineHeight: 1.6,
            }}>
              Sign in to your {company.name || 'SENTHUR CHEMICAL'} dashboard
            </p>
          </div>

          {/* Error / Expired banners */}
          {isExpired && !error && (
            <div style={{
              display: 'flex', alignItems: 'center', gap: '10px',
              background: 'rgba(56, 189, 248, 0.15)', border: '1px solid rgba(56, 189, 248, 0.4)',
              borderRadius: '12px', padding: '12px 14px', marginBottom: '1.5rem',
              color: '#38bdf8',
            }}>
              <AlertCircle size={16} />
              <span style={{ ...ETHNO, fontSize: '0.72rem', letterSpacing: '0.04em' }}>
                Session expired — please sign in again
              </span>
            </div>
          )}
          {error && (
            <div style={{
              display: 'flex', alignItems: 'flex-start', gap: '10px',
              background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)',
              borderRadius: '12px', padding: '12px 14px', marginBottom: '1.5rem',
              color: '#fca5a5',
            }}>
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: '1px' }} />
              <span style={{ ...ETHNO, fontSize: '0.72rem', letterSpacing: '0.04em' }}>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit}>
            {/* Username */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ ...ETHNO, display: 'block', fontSize: '0.68rem', color: 'rgba(255, 255, 255, 0.75)', letterSpacing: '0.1em', marginBottom: '8px' }}>
                USERNAME
              </label>
              <div style={{ position: 'relative' }}>
                <User size={16} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'rgba(255, 255, 255, 0.45)', pointerEvents: 'none' }} />
                <input
                  id="login-username"
                  type="text"
                  className="glass-input"
                  placeholder="Enter your username"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  disabled={isLoading}
                  autoComplete="username"
                />
              </div>
            </div>

            {/* Password */}
            <div style={{ marginBottom: '2rem' }}>
              <label style={{ ...ETHNO, display: 'block', fontSize: '0.68rem', color: 'rgba(255, 255, 255, 0.75)', letterSpacing: '0.1em', marginBottom: '8px' }}>
                PASSWORD
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={16} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'rgba(255, 255, 255, 0.45)', pointerEvents: 'none' }} />
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  className="glass-input"
                  placeholder="Enter your password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  disabled={isLoading}
                  autoComplete="current-password"
                  style={{ paddingRight: '44px' }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(v => !v)}
                  style={{
                    position: 'absolute', right: '12px', top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: '#38bdf8', padding: '4px', display: 'flex', alignItems: 'center',
                  }}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              id="login-submit"
              type="submit"
              className="btn-signin"
              disabled={isLoading}
            >
              {isLoading
                ? <RefreshCw size={18} style={{ animation: 'spin 1s linear infinite' }} />
                : <>
                    <Lock size={16} />
                    SIGN IN
                  </>
              }
            </button>
          </form>

          {/* Footer note */}
          <p style={{
            ...ETHNO,
            textAlign: 'center',
            fontSize: '0.6rem',
            color: 'rgba(255, 255, 255, 0.35)',
            marginTop: '2rem',
            letterSpacing: '0.08em',
          }}>
            AUTHORISED PERSONNEL ONLY · {company.name || 'SENTHUR CHEMICAL'}
          </p>

          {/* Bottom accent line */}
          <div style={{
            marginTop: '1.5rem',
            height: '2px',
            background: 'linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.4), transparent)',
            borderRadius: '1px',
          }} />
        </div>
      </div>
    </div>
  );
};

export default Login;
