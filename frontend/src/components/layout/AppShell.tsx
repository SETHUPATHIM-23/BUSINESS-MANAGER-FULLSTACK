import React, { useState, useEffect } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Handshake,
  Package,
  Receipt,
  FileText,
  ShoppingCart,
  Wallet,
  BarChart3,
  Database,
  Settings,
  Bell,
  Search,
  User as UserIcon,
  Sun,
  Moon,
  LogOut,
  ShieldCheck,
  Menu,
  X
} from 'lucide-react';

import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { UserProfileModal } from '../common/UserProfileModal';

interface SidebarItem {
  name: string;
  path: string;
  icon: React.ComponentType<any>;
  requiredPermission?: string;
}

interface SidebarGroup {
  groupName: string;
  items: SidebarItem[];
}

export const AppShell: React.FC = () => {
  const { user, roles, logout, hasPermission } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);
      if (!mobile) {
        setIsMobileMenuOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Close mobile drawer on route change
  useEffect(() => {
    setIsMobileMenuOpen(false);
  }, [location]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navigationGroups: SidebarGroup[] = [
    {
      groupName: 'Core',
      items: [
        { name: 'Dashboard', path: '/', icon: LayoutDashboard }
      ]
    },
    {
      groupName: 'Operations',
      items: [
        { name: 'Customers', path: '/customers', icon: Users, requiredPermission: 'CUSTOMER_READ' },
        { name: 'Suppliers', path: '/suppliers', icon: Handshake, requiredPermission: 'SUPPLIER_READ' },
        { name: 'Products', path: '/products', icon: Package, requiredPermission: 'PRODUCT_READ' }
      ]
    },
    {
      groupName: 'Transactions',
      items: [
        { name: 'Billing', path: '/billing', icon: Receipt, requiredPermission: 'BILLING_READ' },
        { name: 'Sales', path: '/sales', icon: FileText, requiredPermission: 'BILLING_READ' },
        { name: 'Purchases', path: '/purchases', icon: ShoppingCart, requiredPermission: 'PURCHASE_READ' },
        { name: 'Funds', path: '/funds', icon: Wallet, requiredPermission: 'FUND_READ' },
        { name: 'Employees', path: '/employees', icon: Users, requiredPermission: 'EMPLOYEE_READ' }
      ]
    },
    {
      groupName: 'Finance & System',
      items: [
        { name: 'Reports', path: '/reports', icon: BarChart3, requiredPermission: 'SYSTEM_READ' },
        { name: 'Backups', path: '/backups', icon: Database, requiredPermission: 'SYSTEM_READ' },
        { name: 'Admin Settings', path: '/admin', icon: Settings, requiredPermission: 'SYSTEM_WRITE' }
      ]
    }
  ];

  // Filter items per group based on permissions
  const filteredGroups = navigationGroups
    .map(group => ({
      ...group,
      items: group.items.filter(item => !item.requiredPermission || hasPermission(item.requiredPermission))
    }))
    .filter(group => group.items.length > 0);

  const primaryRole = roles.includes('ROLE_ADMINISTRATOR') ? 'System Administrator' : (roles[0] || 'User');

  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: 'var(--bg-primary)', position: 'relative' }}>
      <UserProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />

      {/* Mobile Drawer Backdrop Overlay */}
      {isMobile && isMobileMenuOpen && (
        <div
          onClick={() => setIsMobileMenuOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(3px)',
            zIndex: 998,
            transition: 'opacity 0.2s ease-in-out'
          }}
        />
      )}

      {/* Sidebar Navigation */}
      <aside
        style={{
          width: '260px',
          borderRight: '1px solid var(--border-sidebar)',
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--bg-sidebar)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          position: 'fixed',
          top: 0,
          left: 0,
          bottom: 0,
          height: '100vh',
          zIndex: isMobile ? 999 : 100,
          overflowY: 'auto',
          transform: isMobile ? (isMobileMenuOpen ? 'translateX(0)' : 'translateX(-100%)') : 'none',
          transition: 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
          boxShadow: isMobile && isMobileMenuOpen ? '4px 0 32px rgba(37,99,235,0.12), 4px 0 8px rgba(0,0,0,0.12)' : '1px 0 0 rgba(255,255,255,0.06)'
        }}
      >
        {/* Sidebar Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid var(--border-sidebar)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                backgroundColor: 'var(--accent-primary)',
                width: '32px',
                height: '32px',
                borderRadius: 'var(--border-radius-sm)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                color: '#ffffff',
                fontSize: '1rem'
              }}
            >
              SMC
            </div>
            <div>
              <h2 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
                SMC Management
              </h2>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-sidebar-heading)', fontWeight: 500 }}>
                ENTERPRISE EDITION
              </span>
            </div>
          </div>

          {/* Close drawer button on mobile */}
          {isMobile && (
            <button
              onClick={() => setIsMobileMenuOpen(false)}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                padding: '4px'
              }}
            >
              <X size={20} />
            </button>
          )}
        </div>

        {/* Sidebar Navigation */}
        <nav style={{ padding: '1rem 0.75rem', flex: 1 }}>
          {filteredGroups.map((group, index) => (
            <div key={index} style={{ marginBottom: '1.25rem' }}>
              <span
                style={{
                  display: 'block',
                  padding: '0 0.75rem',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  color: 'var(--text-sidebar)',
                  opacity: 0.6,
                  letterSpacing: '0.05em',
                  marginBottom: '0.4rem'
                }}
              >
                {group.groupName}
              </span>
              <ul style={{ listStyle: 'none' }}>
                {group.items.map((item, itemIndex) => {
                  const Icon = item.icon;
                  return (
                    <li key={itemIndex} style={{ marginBottom: '0.2rem' }}>
                      <NavLink
                        to={item.path}
                        onClick={() => {
                          if (isMobile) setIsMobileMenuOpen(false);
                        }}
                        style={({ isActive }) => ({
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.75rem',
                          padding: '0.55rem 0.75rem',
                          borderRadius: 'var(--border-radius-sm)',
                          textDecoration: 'none',
                          fontSize: '0.875rem',
                          fontWeight: isActive ? 600 : 400,
                          color: isActive ? 'var(--text-sidebar-active)' : 'var(--text-sidebar)',
                          background: isActive ? 'var(--bg-sidebar-active)' : 'transparent',
                          transition: 'var(--transition-smooth)'
                        })}
                      >
                        <Icon size={17} />
                        <span>{item.name}</span>
                      </NavLink>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div style={{ flex: 1, paddingLeft: isMobile ? 0 : '260px', display: 'flex', flexDirection: 'column', width: '100%', minWidth: 0 }}>
        {/* Header Bar */}
        <header
          style={{
            height: '64px',
            borderBottom: '1px solid var(--border-sidebar)',
            background: 'var(--header-bg)',
            backdropFilter: 'blur(16px) saturate(1.5)',
            WebkitBackdropFilter: 'blur(16px) saturate(1.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: isMobile ? '0 1rem' : '0 1.5rem',
            position: 'sticky',
            top: 0,
            zIndex: 90,
            boxShadow: '0 1px 0 var(--border-color), 0 4px 16px rgba(0,0,0,0.04)'
          }}
        >
          {/* Header Left (Hamburger Toggle + Search) */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flex: 1 }}>
            {isMobile && (
              <button
                onClick={() => setIsMobileMenuOpen(true)}
                style={{
                  background: 'var(--bg-tertiary)',
                  border: '1px solid var(--border-color)',
                  color: 'var(--text-primary)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: '8px',
                  borderRadius: 'var(--border-radius-sm)'
                }}
                aria-label="Toggle menu"
              >
                <Menu size={20} />
              </button>
            )}

            <div style={{ position: 'relative', display: 'flex', alignItems: 'center', maxWidth: isMobile ? '180px' : '300px', width: '100%' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Search..."
                style={{
                  width: '100%',
                  padding: '7px 12px 7px 36px',
                  borderRadius: 'var(--border-radius-sm)',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'var(--bg-tertiary)',
                  color: 'var(--text-primary)',
                  fontSize: '0.85rem',
                  outline: 'none',
                  transition: 'var(--transition-smooth)'
                }}
                onFocus={(e) => (e.target.style.borderColor = 'var(--accent-primary)')}
                onBlur={(e) => (e.target.style.borderColor = 'var(--border-color)')}
              />
            </div>
          </div>

          {/* Header Right */}
          <div style={{ display: 'flex', alignItems: 'center', gap: isMobile ? '0.5rem' : '1rem' }}>
            {/* Theme Toggle Button */}
            <button
              onClick={toggleTheme}
              style={{
                backgroundColor: 'var(--bg-tertiary)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-primary)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 10px',
                borderRadius: 'var(--border-radius-sm)',
                fontSize: '0.8rem',
                fontWeight: 500,
                transition: 'var(--transition-smooth)'
              }}
              title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
            >
              {theme === 'light' ? <Moon size={16} /> : <Sun size={16} />}
              {!isMobile && <span>{theme === 'light' ? 'Dark Mode' : 'Light Mode'}</span>}
            </button>

            {/* Notifications */}
            <button
              style={{
                background: 'var(--bg-tertiary)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
                cursor: 'pointer',
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '7px',
                borderRadius: 'var(--border-radius-sm)',
                transition: 'var(--transition-smooth)'
              }}
            >
              <Bell size={18} />
              <span
                style={{
                  position: 'absolute',
                  top: '3px',
                  right: '3px',
                  width: '7px',
                  height: '7px',
                  backgroundColor: 'var(--danger)',
                  borderRadius: '50%'
                }}
              />
            </button>

            {/* User Profile Badge */}
            <div
              onClick={() => setIsProfileModalOpen(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '4px 6px',
                borderRadius: 'var(--border-radius-sm)',
                cursor: 'pointer',
                borderLeft: isMobile ? 'none' : '1px solid var(--border-color)',
                transition: 'background-color 0.2s'
              }}
              title="Click to edit profile & change password"
            >
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: 'var(--border-radius-sm)',
                  backgroundColor: 'var(--info-bg)',
                  border: '1px solid var(--border-color)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--info-text)'
                }}
              >
                <UserIcon size={16} />
              </div>
              {!isMobile && (
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    {user || 'User'}
                  </span>
                  <span style={{ fontSize: '0.72rem', color: 'var(--success-text)', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <ShieldCheck size={12} /> {primaryRole}
                  </span>
                </div>
              )}
            </div>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              style={{
                background: 'var(--bg-tertiary)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '7px',
                borderRadius: 'var(--border-radius-sm)',
                transition: 'var(--transition-smooth)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--danger-bg)';
                e.currentTarget.style.color = 'var(--danger)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--bg-tertiary)';
                e.currentTarget.style.color = 'var(--text-secondary)';
              }}
              title="Sign Out"
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>

        {/* Content Body */}
        <main style={{ padding: isMobile ? '1rem 0.75rem' : '2rem', flex: 1, display: 'flex', flexDirection: 'column', width: '100%', minWidth: 0 }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};
