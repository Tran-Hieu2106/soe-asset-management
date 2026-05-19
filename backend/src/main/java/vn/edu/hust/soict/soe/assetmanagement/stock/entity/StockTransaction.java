package vn.edu.hust.soict.soe.assetmanagement.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CS-02: Every receipt and issue event
 * CS-03: Balance = SUM(RECEIPT qty) - SUM(ISSUE qty) — computed from this table
 * CS-04: Departmental allocation via requesting_department_id
 *
 * Transactions are IMMUTABLE — no updated_at column per V3 schema comment.
 * Maps to: stock_transactions table (V3__create_stock.sql)
 */
@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_stock_tx_material", columnList = "material_id"),
        @Index(name = "idx_stock_tx_location",  columnList = "storage_location_id"),
        @Index(name = "idx_stock_tx_type",       columnList = "transaction_type"),
        @Index(name = "idx_stock_tx_dept",       columnList = "requesting_department_id"),
        @Index(name = "idx_stock_tx_date",       columnList = "document_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransaction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // Copied from material at time of transaction
    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    // Auto-computed = quantity * unit_price
    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    // CS-04: required when transaction_type = ISSUE
    @Column(name = "requesting_department_id")
    private UUID requestingDepartmentId;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    // Reference document — NOT NULL per schema
    @Column(name = "document_ref", nullable = false, length = 255)
    private String documentRef;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Audit — NO updated_at (immutable)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
        // Auto-calculate total_value
        if (unitPrice != null && quantity != null) {
            this.totalValue = unitPrice.multiply(quantity);
        }
    }
}
