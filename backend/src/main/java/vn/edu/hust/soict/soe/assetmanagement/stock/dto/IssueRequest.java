package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Request DTO for POST /api/stock/issue  (CS-02 + CS-04) */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IssueRequest {

    @NotNull(message = "ID vật tư không được null")
    private UUID materialId;

    @NotNull(message = "ID kho không được null")
    private UUID storageLocationId;

    @NotNull(message = "Số lượng không được null")
    @DecimalMin(value = "0.001", message = "Số lượng phải > 0")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    // CS-04: required for ISSUE
    @NotNull(message = "ID phòng ban không được null")
    private UUID requestingDepartmentId;

    private String requestedBy;

    @NotBlank(message = "Số chứng từ không được để trống")
    private String documentRef;

    @NotNull(message = "Ngày chứng từ không được null")
    private LocalDate documentDate;

    private String notes;
    private String approvedBy;
}
