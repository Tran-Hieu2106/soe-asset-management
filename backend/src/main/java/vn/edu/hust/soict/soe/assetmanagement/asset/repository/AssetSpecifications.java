package vn.edu.hust.soict.soe.assetmanagement.asset.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AssetSpecifications {

    private AssetSpecifications() {}

    public static Specification<FixedAsset> filter(
            AssetStatus status,
            Integer categoryId,
            UUID managingUnitId,
            LocalDate acquisitionFrom,
            LocalDate acquisitionTo,
            String keyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (managingUnitId != null) {
                predicates.add(cb.equal(root.get("managingUnitId"), managingUnitId));
            }
            if (acquisitionFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("acquisitionDate"), acquisitionFrom));
            }
            if (acquisitionTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("acquisitionDate"), acquisitionTo));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("assetCode")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
