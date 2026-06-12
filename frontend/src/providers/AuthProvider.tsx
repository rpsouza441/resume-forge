'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { authApi } from '@/lib/api';
import type { AuthResponse, CurrentUser } from '@/types';

interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: CurrentUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<CurrentUser | null>(null);

  const loadUser = useCallback(async () => {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      setIsLoading(false);
      return;
    }
    try {
      const currentUser = await authApi.me();
      setUser(currentUser);
      setIsAuthenticated(true);
    } catch {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = async (email: string, password: string) => {
    const response: AuthResponse = await authApi.login({ email, password });
    localStorage.setItem('auth_token', response.accessToken);
    localStorage.setItem('auth_user', JSON.stringify(response.user));
    setUser({
      id: response.user.id,
      name: response.user.name,
      email: response.user.email,
      createdAt: new Date().toISOString(),
      lastLoginAt: new Date().toISOString(),
    });
    setIsAuthenticated(true);
  };

  const register = async (name: string, email: string, password: string) => {
    const response: AuthResponse = await authApi.register({ name, email, password });
    localStorage.setItem('auth_token', response.accessToken);
    localStorage.setItem('auth_user', JSON.stringify(response.user));
    setUser({
      id: response.user.id,
      name: response.user.name,
      email: response.user.email,
      createdAt: new Date().toISOString(),
      lastLoginAt: new Date().toISOString(),
    });
    setIsAuthenticated(true);
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    } finally {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      setUser(null);
      setIsAuthenticated(false);
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
