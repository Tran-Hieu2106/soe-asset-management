package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for `assets` table. Uses UUID as Primary Key per DB schema.
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, UUID> {
    Optional<FixedAsset> findByAssetCode(String assetCode);
    boolean existsByAssetCode(String assetCode);
}