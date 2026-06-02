package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, UUID>, JpaSpecificationExecutor<FixedAsset> {
    Optional<FixedAsset> findByAssetCode(String assetCode);
    boolean existsByAssetCode(String assetCode);
}
