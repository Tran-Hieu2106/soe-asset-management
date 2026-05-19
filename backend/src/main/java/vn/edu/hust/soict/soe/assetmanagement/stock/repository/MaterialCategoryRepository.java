package vn.edu.hust.soict.soe.assetmanagement.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import java.util.Optional;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Integer> {
    Optional<MaterialCategory> findByCode(String code);
}
