package vn.edu.hust.soict.soe.assetmanagement.asset.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asset_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "useful_life_min")
    private Integer usefulLifeMin;

    @Column(name = "useful_life_max")
    private Integer usefulLifeMax;

    @Column(name = "depreciation_method", nullable = false, length = 20)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE";

    @Column(columnDefinition = "TEXT")
    private String description;
}
