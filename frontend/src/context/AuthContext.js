import { createContext, useContext, useState, useCallback } from 'react';
import { userService } from '../services/userService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(false);

  const isAuthenticated = !!token && !!user;

  const login = useCallback(async (username, password) => {
    setLoading(true);
    try {
      const res = await userService.login(username, password);
      const { token: jwt, ...userInfo } = res.data;
      localStorage.setItem('token', jwt);
      localStorage.setItem('user', JSON.stringify(userInfo));
      setToken(jwt);
      setUser(userInfo);
      return { success: true };
    } catch (err) {
      return { success: false, message: err.message || '登录失败，请重试' };
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (data) => {
    setLoading(true);
    try {
      await userService.register(data);
      return { success: true };
    } catch (err) {
      return { success: false, message: err.message || '注册失败，请重试' };
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  const fetchProfile = useCallback(async () => {
    if (!token) return;
    try {
      const res = await userService.me();
      setUser(res.data);
      localStorage.setItem('user', JSON.stringify(res.data));
    } catch (err) {
      if (err.code === 401) logout();
    }
  }, [token, logout]);

  return (
    <AuthContext.Provider value={{
      user,
      token,
      isAuthenticated,
      loading,
      login,
      register,
      logout,
      fetchProfile,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
