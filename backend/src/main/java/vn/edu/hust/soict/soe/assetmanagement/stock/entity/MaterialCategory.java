package vn.edu.hust.soict.soe.assetmanagement.stock.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "material_categories")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
