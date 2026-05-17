package vn.edu.hust.soict.soe.assetmanagement.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    boolean existsByMaterialCode(String materialCode);

    Optional<Material> findByMaterialCode(String materialCode);

    // Paginated list of active materials
    Page<Material> findByIsActiveTrue(Pageable pageable);

    // Filtered by category
    Page<Material> findByCategoryIdAndIsActiveTrue(Integer categoryId, Pageable pageable);

    // Search by name (case-insensitive)
    @Query("""
        SELECT m FROM Material m
        WHERE m.isActive = true
          AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY m.name ASC
        """)
    List<Material> searchByName(@Param("keyword") String keyword);
}
