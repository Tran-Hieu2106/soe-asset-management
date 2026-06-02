import axiosInstance from './axiosInstance';
import type { ApiResponse, PageResponse } from '../types/common.types';
import type { AssetHistory, AssetStatus, FixedAsset } from '../types/asset.types';

export interface AssetQuery {
  page?: number;
  size?: number;
  status?: AssetStatus;
  categoryId?: number;
  managingUnitId?: string;
  acquisitionFrom?: string;
  acquisitionTo?: string;
  keyword?: string;
}

export const assetApi = {
  list: (params: AssetQuery) =>
    axiosInstance
      .get<ApiResponse<PageResponse<FixedAsset>>>('/assets', { params })
      .then(r => r.data.data),

  getById: (id: string) =>
    axiosInstance.get<ApiResponse<FixedAsset>>(`/assets/${id}`).then(r => r.data.data),

  create: (data: Partial<FixedAsset>) =>
    axiosInstance.post<ApiResponse<FixedAsset>>('/assets', data).then(r => r.data.data),

  update: (id: string, data: Partial<FixedAsset>) =>
    axiosInstance.put<ApiResponse<FixedAsset>>(`/assets/${id}`, data).then(r => r.data.data),

  updateStatus: (id: string, newStatus: AssetStatus, reason: string) =>
    axiosInstance
      .patch<ApiResponse<FixedAsset>>(`/assets/${id}/status`, null, { params: { newStatus, reason } })
      .then(r => r.data.data),

  history: (id: string) =>
    axiosInstance.get<ApiResponse<AssetHistory[]>>(`/assets/${id}/history`).then(r => r.data.data),
};
