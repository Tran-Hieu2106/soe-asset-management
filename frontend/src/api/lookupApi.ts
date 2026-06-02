import axiosInstance from './axiosInstance';
import type { ApiResponse, LookupItem } from '../types/common.types';

export const lookupApi = {
  managingUnits: () =>
    axiosInstance.get<ApiResponse<LookupItem[]>>('/lookups/managing-units').then(r => r.data.data),
  assetCategories: () =>
    axiosInstance.get<ApiResponse<LookupItem[]>>('/lookups/asset-categories').then(r => r.data.data),
  materialCategories: () =>
    axiosInstance.get<ApiResponse<LookupItem[]>>('/lookups/material-categories').then(r => r.data.data),
  storageLocations: () =>
    axiosInstance.get<ApiResponse<LookupItem[]>>('/lookups/storage-locations').then(r => r.data.data),
};
