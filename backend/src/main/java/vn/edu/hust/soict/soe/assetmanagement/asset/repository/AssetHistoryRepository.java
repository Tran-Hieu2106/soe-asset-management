package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;

import java.util.List;
import java.util.UUID;

/**
 * Repository for `asset_history`. 
 * Required to fetch the chronological ledger for FA-04.
 */
@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, UUID> {
    List<AssetHistory> findByAssetIdOrderByPerformedAtDesc(UUID assetId);
}