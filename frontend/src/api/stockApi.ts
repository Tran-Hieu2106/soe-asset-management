import axiosInstance from './axiosInstance';
import type { ApiResponse, PageResponse } from '../types/common.types';

export interface Material {
  id: string;
  materialCode: string;
  name: string;
  categoryId: number;
  categoryName?: string;
  unitOfMeasure: string;
  unitPrice?: number;
  minimumStock?: number;
  isActive?: boolean;
}

export interface StockBalance {
  materialId: string;
  materialCode: string;
  materialName: string;
  storageLocationId: string;
  storageLocationName: string;
  unitOfMeasure: string;
  currentBalance: number;
  minimumStock: number;
  isBelowMinimum: boolean;
}

export interface ReceiptPayload {
  materialId: string;
  storageLocationId: string;
  quantity: number;
  unitPrice?: number;
  documentRef: string;
  documentDate: string;
  notes?: string;
}

export interface IssuePayload {
  materialId: string;
  storageLocationId: string;
  quantity: number;
  requestingDepartmentId: string;
  documentRef: string;
  documentDate: string;
  requestedBy?: string;
  notes?: string;
}

export const stockApi = {
  materials: (params?: { page?: number; size?: number; categoryId?: number }) =>
    axiosInstance.get<ApiResponse<PageResponse<Material>>>('/materials', { params }).then(r => r.data.data),

  searchMaterials: (keyword: string) =>
    axiosInstance.get<ApiResponse<Material[]>>('/materials/search', { params: { keyword } }).then(r => r.data.data),

  createMaterial: (data: Record<string, unknown>) =>
    axiosInstance.post<ApiResponse<Material>>('/materials', data).then(r => r.data.data),

  receipt: (data: ReceiptPayload) =>
    axiosInstance.post('/stock/receipt', data).then(r => r.data),

  issue: (data: IssuePayload) =>
    axiosInstance.post('/stock/issue', data).then(r => r.data),

  balance: () =>
    axiosInstance.get<ApiResponse<StockBalance[]>>('/stock/balance').then(r => r.data.data),

  usage: (startDate?: string, endDate?: string) =>
    axiosInstance
      .get<ApiResponse<unknown[]>>('/stock/usage', { params: { startDate, endDate } })
      .then(r => r.data.data),
};
