package vn.edu.hust.soict.soe.assetmanagement.liquidation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationRequest;
import vn.edu.hust.soict.soe.assetmanagement.liquidation.entity.LiquidationStatus;

import java.util.UUID;

@Repository
public interface LiquidationRepository extends JpaRepository<LiquidationRequest, UUID> {
    
    // Validates the business rule: blocks concurrent pending liquidation requests for the same asset
    boolean existsByAssetIdAndStatus(UUID assetId, LiquidationStatus status);
}