import axiosInstance from './axiosInstance';
import type { ApiResponse, PageResponse } from '../types/common.types';

export type HandoverStatus =
  | 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'CONFIRMED' | 'COMPLETED' | 'REJECTED';

export interface Handover {
  id: string;
  requestCode: string;
  assetId: string;
  fromUnitId: string;
  toUnitId: string;
  reason: string;
  handoverDate?: string;
  assetCondition?: string;
  status: HandoverStatus;
  initiatedBy: string;
  notes?: string;
  documentRef?: string;
  createdAt?: string;
}

export interface CreateHandoverPayload {
  assetId: string;
  fromUnitId: string;
  toUnitId: string;
  reason: string;
  handoverDate?: string;
  assetCondition?: string;
  notes?: string;
}

export const handoverApi = {
  list: (page = 0, size = 20) =>
    axiosInstance.get<ApiResponse<PageResponse<Handover>>>('/handovers', { params: { page, size } }).then(r => r.data.data),

  getById: (id: string) =>
    axiosInstance.get<ApiResponse<Handover>>(`/handovers/${id}`).then(r => r.data.data),

  create: (data: CreateHandoverPayload) =>
    axiosInstance.post<ApiResponse<Handover>>('/handovers', data).then(r => r.data.data),

  submit: (id: string) => axiosInstance.put<ApiResponse<Handover>>(`/handovers/${id}/submit`).then(r => r.data.data),
  approve: (id: string, notes?: string) =>
    axiosInstance.put<ApiResponse<Handover>>(`/handovers/${id}/approve`, null, { params: { notes } }).then(r => r.data.data),
  confirm: (id: string, notes?: string) =>
    axiosInstance.put<ApiResponse<Handover>>(`/handovers/${id}/confirm`, null, { params: { notes } }).then(r => r.data.data),
  complete: (id: string) =>
    axiosInstance.put<ApiResponse<Handover>>(`/handovers/${id}/complete`).then(r => r.data.data),
  reject: (id: string, reason: string) =>
    axiosInstance.put<ApiResponse<Handover>>(`/handovers/${id}/reject`, null, { params: { reason } }).then(r => r.data.data),

  downloadDocument: (id: string) =>
    axiosInstance.get(`/handovers/${id}/document`, { responseType: 'blob' }).then(r => r.data as Blob),
};
