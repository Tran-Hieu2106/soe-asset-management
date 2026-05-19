package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for Material (CS-01) */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialDto {
    private UUID          id;
    private String        materialCode;
    private String        name;
    private Integer       categoryId;
    private String        categoryName;
    private String        unitOfMeasure;
    private String        technicalSpecs;
    private String        supplierName;
    private String        supplierCode;
    private BigDecimal    unitPrice;
    private BigDecimal    minimumStock;
    private Boolean       isActive;
    private String        notes;
    private LocalDateTime createdAt;
    private String        createdBy;
}
