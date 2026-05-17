package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import lombok.*;
import java.math.BigDecimal;

/** Request DTO for PUT /api/materials/{id} — all fields optional */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateMaterialRequest {
    private String     name;
    private Integer    categoryId;
    private String     unitOfMeasure;
    private String     technicalSpecs;
    private String     supplierName;
    private String     supplierCode;
    private BigDecimal unitPrice;
    private BigDecimal minimumStock;
    private Boolean    isActive;
    private String     notes;
}

// ── VALIDATION RULES ────────────────────────────────────────────────────────────
@Size(min = 1, message = "Tên vật tư không được rỗng")
private String name;

@DecimalMin(value = "0.0", message = "Tồn kho tối thiểu phải >= 0")
private BigDecimal minimumStock;