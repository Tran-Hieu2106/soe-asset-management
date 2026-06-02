package vn.edu.hust.soict.soe.assetmanagement.asset.enums;

/**
 * ==============================================================================
 * ENUM: AssetStatus
 * PURPOSE: Defines the strict lifecycle states a fixed asset can occupy (FA-03).
 * INTEGRATION: 
 * - TRANSFERRED is set by M4 (HandoverModule) upon approval.
 * - LIQUIDATED is set by M4 (LiquidationModule) upon approval.
 * ==============================================================================
 */
public enum AssetStatus {
    IN_USE, 
    MAINTENANCE, 
    IDLE, 
    TRANSFERRED, 
    LIQUIDATED
}