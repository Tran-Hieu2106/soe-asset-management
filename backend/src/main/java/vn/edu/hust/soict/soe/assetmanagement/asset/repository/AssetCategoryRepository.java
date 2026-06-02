package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetCategory;

import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Integer> {
    Optional<AssetCategory> findByCode(String code);
}
