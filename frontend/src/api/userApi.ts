import axiosInstance from './axiosInstance';
import type { ApiResponse } from '../types/common.types';
import type { CurrentUser } from '../store/authStore';

export interface CreateUserPayload {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  phone?: string;
  roleCode: string;
}

export const userApi = {
  list: () =>
    axiosInstance.get<ApiResponse<CurrentUser[]>>('/users').then(r => r.data.data),

  getById: (id: string) =>
    axiosInstance.get<ApiResponse<CurrentUser>>(`/users/${id}`).then(r => r.data.data),

  create: (data: CreateUserPayload) =>
    axiosInstance.post<ApiResponse<CurrentUser>>('/users', data).then(r => r.data.data),

  deactivate: (id: string) =>
    axiosInstance.patch<ApiResponse<void>>(`/users/${id}/deactivate`).then(r => r.data),
};
