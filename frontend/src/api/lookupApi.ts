import axiosInstance from './axiosInstance';
import type { ApiResponse, LookupItem } from '../types/common.types';

/*
Four simple GET calls that return LookupItem[] arrays
used to populate dropdowns throughout the app: 
managingUnits, assetCategories, materialCategories, storageLocations.
*/

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
