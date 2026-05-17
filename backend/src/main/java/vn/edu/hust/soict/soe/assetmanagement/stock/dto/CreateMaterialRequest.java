package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/** Request DTO for POST /api/materials */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateMaterialRequest {

    @NotBlank(message = "Mã vật tư không được để trống")
    @Pattern(regexp = "^[A-Z0-9_-]+$",
             message = "Mã vật tư chỉ gồm chữ hoa, số, dấu - và _")
    private String materialCode;

    @NotBlank(message = "Tên vật tư không được để trống")
    private String name;

    @NotNull(message = "Danh mục không được null")
    private Integer categoryId;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unitOfMeasure;

    private String     technicalSpecs;
    private String     supplierName;
    private String     supplierCode;
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "Tồn kho tối thiểu phải >= 0")
    @Builder.Default
    private BigDecimal minimumStock = BigDecimal.ZERO;

    private String notes;
}
