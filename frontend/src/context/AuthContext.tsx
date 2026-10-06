import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { api, setAccessToken, setRefreshTokenInCookie, getRefreshTokenFromCookie } from '../utils/api';

export interface AuthContextType {
  user: string | null;
  roles: string[];
  permissions: string[];
  isAuthenticated: boolean;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
  updateProfile: (username: string, newPassword?: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<string | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const initAuth = async () => {
      const refreshToken = getRefreshTokenFromCookie();
      if (refreshToken) {
        try {
          const refreshRes = await api.post('/api/auth/refresh', { refreshToken });
          if (refreshRes.data?.accessToken) {
            setAccessToken(refreshRes.data.accessToken);
            const meRes = await api.get('/api/auth/me');
            setUser(meRes.data.username);
            setRoles(meRes.data.roles || []);
            setPermissions(meRes.data.permissions || []);
            setIsAuthenticated(true);
          }
        } catch (err) {
          console.error('Session restore failed:', err);
          logout();
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (username: string, password: string) => {
    const response = await api.post('/api/auth/login', { username, password });
    const { accessToken, refreshToken, username: authUser, roles: authRoles, permissions: authPerms } = response.data;

    setAccessToken(accessToken);
    setRefreshTokenInCookie(refreshToken);

    setUser(authUser);
    setRoles(authRoles || []);
    setPermissions(authPerms || []);
    setIsAuthenticated(true);
  };

  const logout = () => {
    setAccessToken('');
    setRefreshTokenInCookie('');
    setUser(null);
    setRoles([]);
    setPermissions([]);
    setIsAuthenticated(false);
  };

  const hasPermission = (permission: string) => {
    if (!isAuthenticated) return false;
    if (roles.includes('ROLE_ADMINISTRATOR')) return true;
    return permissions.includes(permission);
  };

  const updateProfile = async (username: string, newPassword?: string) => {
    const response = await api.put('/api/auth/profile', {
      username,
      newPassword: newPassword || undefined
    });

    const { accessToken, refreshToken, username: authUser, roles: authRoles, permissions: authPerms } = response.data;
    if (accessToken) setAccessToken(accessToken);
    if (refreshToken) setRefreshTokenInCookie(refreshToken);

    setUser(authUser);
    setRoles(authRoles || []);
    setPermissions(authPerms || []);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        roles,
        permissions,
        isAuthenticated,
        loading,
        login,
        logout,
        hasPermission,
        updateProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
