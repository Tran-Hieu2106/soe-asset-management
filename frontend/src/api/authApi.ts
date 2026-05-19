import axiosInstance from './axiosInstance';
import type { CurrentUser } from '../store/authStore';

// ── Types ─────────────────────────────────────────────────────

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
}

// ── Auth API ──────────────────────────────────────────────────

export const authApi = {

  login: (data: LoginRequest) =>
    axiosInstance
      .post<{ success: boolean; message: string; data: LoginResponse }>
        ('/auth/login', data)
      .then(r => r.data.data),

  getMe: () =>
    axiosInstance
      .get<{ success: boolean; message: string; data: CurrentUser }>
        ('/users/me')
      .then(r => r.data.data),
};
