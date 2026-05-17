package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Request DTO for POST /api/stock/receipt  (CS-02) */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReceiptRequest {

    @NotNull(message = "ID vật tư không được null")
    private UUID materialId;

    @NotNull(message = "ID kho không được null")
    private UUID storageLocationId;

    @NotNull(message = "Số lượng không được null")
    @DecimalMin(value = "0.001", message = "Số lượng phải > 0")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    @NotBlank(message = "Số chứng từ không được để trống")
    private String documentRef;

    @NotNull(message = "Ngày chứng từ không được null")
    private LocalDate documentDate;

    private String notes;
    private String approvedBy;
}
