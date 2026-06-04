package vn.edu.hust.soict.soe.assetmanagement.asset.enums;

/**
 * Enumeration for asset lifecycle statuses.
 * IN_USE: Asset is currently being used.
 * MAINTENANCE: Asset is under maintenance and not available for use.
 * IDLE: Asset is not in use but available.
 * TRANSFERRED: Asset has been transferred to another department or location.
 * LIQUIDATED: Asset has been disposed of or sold.
 */

public enum AssetStatus {
    IN_USE, 
    MAINTENANCE, 
    IDLE, 
    TRANSFERRED, 
    LIQUIDATED
}