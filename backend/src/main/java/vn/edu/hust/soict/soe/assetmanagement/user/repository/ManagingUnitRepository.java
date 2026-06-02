package vn.edu.hust.soict.soe.assetmanagement.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.ManagingUnit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagingUnitRepository extends JpaRepository<ManagingUnit, UUID> {
    Optional<ManagingUnit> findByCode(String code);
    List<ManagingUnit> findByIsActiveTrueOrderByCodeAsc();
}
