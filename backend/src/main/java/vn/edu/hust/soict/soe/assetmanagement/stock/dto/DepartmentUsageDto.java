package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for GET /api/stock/usage  (CS-04)
 * Department-wise consumption summary.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentUsageDto {
    private UUID       departmentId;
    private UUID       materialId;
    private String     materialCode;
    private String     materialName;
    private String     unitOfMeasure;
    private BigDecimal totalIssued;
    private BigDecimal totalValue;
}
