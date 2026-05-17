package vn.edu.hust.soict.soe.assetmanagement.stock.dto;

import lombok.*;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for StockTransaction */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransactionDto {
    private UUID            id;
    private UUID            materialId;
    private String          materialCode;
    private String          materialName;
    private UUID            storageLocationId;
    private String          storageLocationName;
    private TransactionType transactionType;
    private BigDecimal      quantity;
    private String          unitOfMeasure;
    private BigDecimal      unitPrice;
    private BigDecimal      totalValue;
    private UUID            requestingDepartmentId;
    private String          requestedBy;
    private String          documentRef;
    private LocalDate       documentDate;
    private String          notes;
    private String          approvedBy;
    private LocalDateTime   approvedAt;
    private LocalDateTime   createdAt;
    private String          createdBy;
}
