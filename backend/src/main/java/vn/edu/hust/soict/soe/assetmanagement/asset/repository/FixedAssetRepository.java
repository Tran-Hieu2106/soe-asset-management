package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
/**
 * Fixed asset repository.
 * Provides CRUD operations and custom queries for the `assets` table.
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, UUID> {
    
    Optional<FixedAsset> findByAssetCode(String assetCode);
    
    boolean existsByAssetCode(String assetCode);
}
