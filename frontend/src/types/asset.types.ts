export type AssetStatus = 'IN_USE' | 'MAINTENANCE' | 'IDLE' | 'TRANSFERRED' | 'LIQUIDATED';

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
  notes?: string;
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
