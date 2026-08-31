import { api } from './api';
import { AuthResponse, UserProfile } from '../types/auth';

export const authService = {
  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await api.post('/auth/login', { email, password });
    const data = res.data.data;
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);
    return data;
  },

  async getCurrentUser(): Promise<UserProfile> {
    const res = await api.get('/auth/me');
    return res.data.data;
  },

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
  }
};
