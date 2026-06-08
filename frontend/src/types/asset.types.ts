export type AssetStatus = 'IN_USE' | 'MAINTENANCE' | 'IDLE' | 'TRANSFERRED' | 'LIQUIDATED';

/*
Defines AssetStatus (5 possible values: IN_USE, MAINTENANCE, IDLE, TRANSFERRED, LIQUIDATED) 
and the full FixedAsset interface — a large object covering the asset's identity (code, name, category), 
financials (originalCost, netBookValue, depreciation fields), lifecycle metadata (status, statusChangedAt, createdBy), and more. 
Also defines AssetHistory for the lifecycle event log shown on the detail page.
*/

export interface FixedAsset {
  id: string;
  assetCode: string;
  name: string;
  categoryId: number;
  categoryCode?: string;
  categoryName?: string;
  managingUnitId: string;
  managingUnitCode?: string;
  managingUnitName?: string;
  serialNumber?: string;
  manufacturer?: string;
  model?: string;
  countryOfOrigin?: string;
  technicalSpecs?: string;
  location?: string;
  originalCost: number;
  acquisitionDate: string;
  fundingSource?: string;
  usefulLifeYears: number;
  salvageValue?: number;
  depreciationMethod?: string;
  accumulatedDepreciation?: number;
  netBookValue?: number;
  annualDepreciationAmount?: number;
  annualDepreciationRate?: number;
  depreciationStartDate?: string;
  depreciationEndDate?: string;
  status: AssetStatus;
  statusReason?: string;
  statusChangedAt?: string;
  statusChangedBy?: string;
  purchaseDocumentRef?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface AssetHistory {
  id: string;
  assetId: string;
  eventType: string;
  description: string;
  oldValue?: string;
  newValue?: string;
  performedBy: string;
  performedAt: string;
}
