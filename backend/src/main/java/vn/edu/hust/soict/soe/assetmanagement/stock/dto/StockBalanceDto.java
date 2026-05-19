package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for GET /api/stock/balance  (CS-03)
 * Balance is computed on-the-fly — no stored column.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockBalanceDto {
    private UUID       materialId;
    private String     materialCode;
    private String     materialName;
    private UUID       storageLocationId;
    private String     storageLocationName;
    private String     unitOfMeasure;
    private BigDecimal currentBalance;
    private BigDecimal minimumStock;
    private Boolean    isBelowMinimum;   // currentBalance < minimumStock
}
