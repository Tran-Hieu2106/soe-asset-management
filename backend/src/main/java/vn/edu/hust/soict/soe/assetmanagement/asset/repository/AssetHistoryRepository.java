package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
/**
 * Asset history repository.
 * Fetches chronological event logs for a specific asset.
 */
@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, UUID> {
    List<AssetHistory> findByAssetIdOrderByPerformedAtDesc(UUID assetId);
}