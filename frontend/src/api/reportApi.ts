import axiosInstance from './axiosInstance';
import type { ApiResponse, PageResponse } from '../types/common.types';

/*
Defines the AuditLog interface. 
Methods: assetReport (paginated table), stockReport, exportAssets(format, params) 
(returns Blob — the responseType: 'blob' tells Axios to keep it as binary), 
exportStock, and auditLogs (paginated, filterable by module and performer).
*/

export interface AssetReportRow {
  assetId: string;
  assetCode: string;
  assetName: string;
  categoryName?: string;
  managingUnitName?: string;
  status: string;
  acquisitionDate?: string;
  originalCost: number;
  accumulatedDepreciation: number;
  netBookValue: number;
}

export interface AuditLog {
  id: string;
  module: string;
  action: string;
  recordCode?: string;
  description?: string;
  performedBy: string;
  performedAt: string;
}

export const reportApi = {
  assetReport: (params: Record<string, unknown>) =>
    axiosInstance.get<ApiResponse<PageResponse<AssetReportRow>>>('/reports/assets', { params }).then(r => r.data.data),

  stockReport: (startDate?: string, endDate?: string) =>
    axiosInstance.get<ApiResponse<unknown[]>>('/reports/stock', { params: { startDate, endDate } }).then(r => r.data.data),

  exportAssets: (format: 'EXCEL' | 'PDF' | 'CSV', params: Record<string, unknown>) =>
    axiosInstance.get('/reports/assets/export', { params: { format, ...params }, responseType: 'blob' }).then(r => r.data as Blob),

  exportStock: (startDate?: string, endDate?: string) =>
    axiosInstance.get('/reports/stock/export', { params: { format: 'EXCEL', startDate, endDate }, responseType: 'blob' }).then(r => r.data as Blob),

  auditLogs: (params: Record<string, unknown>) =>
    axiosInstance.get<ApiResponse<PageResponse<AuditLog>>>('/audit-logs', { params }).then(r => r.data.data),
};
