import axiosInstance from './axiosInstance';
import type { CurrentUser } from '../store/authStore';

/*
Two methods: login(data) posts to /auth/login and returns the token + username. 
getMe() gets /users/me and normalises the roles and managingUnitCodes fields 
from whatever the backend sends (Set or Array) into plain arrays.
*/
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
      .get<{ success: boolean; message: string; data: CurrentUser & { roles?: string[] | Set<string> } }>
        ('/users/me')
      .then(r => {
        const d = r.data.data;
        return {
          ...d,
          roles: Array.isArray(d.roles) ? d.roles : [...(d.roles ?? [])],
          managingUnitCodes: Array.isArray(d.managingUnitCodes)
            ? d.managingUnitCodes
            : [...(d.managingUnitCodes ?? [])],
        } as CurrentUser;
      }),
};
