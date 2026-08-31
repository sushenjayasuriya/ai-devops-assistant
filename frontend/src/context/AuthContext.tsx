import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserProfile, UserRole } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, pass: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: UserRole) => boolean;
  isAdmin: boolean;
  isDevops: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      authService.getCurrentUser()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email: string, pass: string) => {
    await authService.login(email, pass);
    const profile = await authService.getCurrentUser();
    setUser(profile);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const hasRole = (role: UserRole) => {
    return user?.roles.includes(role) || false;
  };

  const isAdmin = hasRole('ADMIN');
  const isDevops = hasRole('DEVOPS_ENGINEER') || isAdmin;

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasRole, isAdmin, isDevops }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
