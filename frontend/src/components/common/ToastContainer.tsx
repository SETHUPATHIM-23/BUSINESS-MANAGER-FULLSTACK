import React, { useState, useEffect } from 'react';
import { toastEvents } from '../../utils/toast';
import type { ToastMessage } from '../../utils/toast';
import { X, CheckCircle, AlertTriangle, AlertCircle, Info } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    const unsubscribe = toastEvents.subscribe((toast) => {
      setToasts((prev) => [...prev, toast]);
      
      // Auto close toast
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== toast.id));
      }, toast.duration || 4000);
    });

    return unsubscribe;
  }, []);

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'success':
        return <CheckCircle size={18} style={{ color: 'var(--success)' }} />;
      case 'warning':
        return <AlertTriangle size={18} style={{ color: 'var(--warning)' }} />;
      case 'error':
        return <AlertCircle size={18} style={{ color: 'var(--danger)' }} />;
      default:
        return <Info size={18} style={{ color: 'var(--accent-primary)' }} />;
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: '20px',
        right: '20px',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        maxWidth: '380px',
        width: '100%'
      }}
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px',
            padding: '14px 16px',
            backgroundColor: 'var(--bg-secondary)',
            color: 'var(--text-primary)',
            borderRadius: 'var(--border-radius)',
            border: '1px solid var(--border-color)',
            boxShadow: 'var(--shadow-lg)',
            animation: 'slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            borderLeft: `4px solid ${
              toast.type === 'success'
                ? 'var(--success)'
                : toast.type === 'warning'
                ? 'var(--warning)'
                : toast.type === 'error'
                ? 'var(--danger)'
                : 'var(--accent-primary)'
            }`
          }}
        >
          <div style={{ marginTop: '2px', flexShrink: 0 }}>{getIcon(toast.type)}</div>
          <div style={{ flex: 1, fontSize: '0.88rem', color: 'var(--text-primary)', lineHeight: '1.35rem', wordBreak: 'break-word', fontWeight: 500 }}>
            {toast.message}
          </div>
          <button
            onClick={() => removeToast(toast.id)}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              padding: '2px',
              borderRadius: '4px',
              transition: 'var(--transition-smooth)',
              flexShrink: 0
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
          >
            <X size={16} />
          </button>
        </div>
      ))}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
      `}} />
    </div>
  );
};
export default ToastContainer;
