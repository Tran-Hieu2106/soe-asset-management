package vn.edu.hust.soict.soe.assetmanagement.handover.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;

import java.util.UUID;

/**
 * Handover repository (HL-01).
 */
@Repository
public interface HandoverRepository extends JpaRepository<HandoverRequest, UUID> {
    
    // Validates the business rule: blocks concurrent pending requests for the same asset
    boolean existsByAssetIdAndStatus(UUID assetId, HandoverStatus status);
}