import axiosInstance from './axiosInstance';
import type { ApiResponse, PageResponse } from '../types/common.types';

export type LiquidationStatus =
  | 'DRAFT' | 'PENDING_MANAGER' | 'PENDING_DIRECTOR' | 'APPROVED' | 'COMPLETED' | 'REJECTED';

export interface Liquidation {
  id: string;
  requestCode: string;
  assetId: string;
  requestingUnitId: string;
  justification: string;
  assetCondition: string;
  currentMarketValue?: number;
  disposalMethod: string;
  status: LiquidationStatus;
  initiatedBy: string;
  finalDisposalValue?: number;
}

export const liquidationApi = {
  list: (page = 0, size = 20) =>
    axiosInstance.get<ApiResponse<PageResponse<Liquidation>>>('/liquidations', { params: { page, size } }).then(r => r.data.data),

  getById: (id: string) =>
    axiosInstance.get<ApiResponse<Liquidation>>(`/liquidations/${id}`).then(r => r.data.data),

  create: (data: Record<string, unknown>) =>
    axiosInstance.post<ApiResponse<Liquidation>>('/liquidations', data).then(r => r.data.data),

  submit: (id: string) => axiosInstance.put(`/liquidations/${id}/submit`).then(r => r.data),
  approveManager: (id: string, notes?: string) =>
    axiosInstance.put(`/liquidations/${id}/approve-manager`, null, { params: { notes } }).then(r => r.data),
  approveDirector: (id: string, notes?: string) =>
    axiosInstance.put(`/liquidations/${id}/approve-director`, null, { params: { notes } }).then(r => r.data),
  complete: (id: string, finalDisposalValue?: number) =>
    axiosInstance.put(`/liquidations/${id}/complete`, null, { params: { finalDisposalValue } }).then(r => r.data),
  reject: (id: string, reason: string) =>
    axiosInstance.put(`/liquidations/${id}/reject`, null, { params: { reason } }).then(r => r.data),
};
