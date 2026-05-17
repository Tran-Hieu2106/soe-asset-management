package vn.edu.hust.soict.soe.assetmanagement.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.DepartmentUsageDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockBalanceDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    /** CS-02: History for one material */
    List<StockTransaction> findByMaterialIdOrderByDocumentDateDesc(UUID materialId);

    // ── CS-03: On-the-fly balance queries ────────────────────────────────

    /**
     * Available stock for one material at one location.
     * Used in StockTransactionService to guard against over-issuing.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.RECEIPT
                 THEN t.quantity ELSE -t.quantity END
        ), 0)
        FROM StockTransaction t
        WHERE t.material.id        = :materialId
          AND t.storageLocation.id = :locationId
        """)
    BigDecimal checkAvailableStock(
            @Param("materialId") UUID materialId,
            @Param("locationId") UUID locationId);

    /**
     * Balance for ALL materials across ALL locations (GET /api/stock/balance)
     */
    @Query("""
        SELECT new vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockBalanceDto(
            t.material.id,
            t.material.materialCode,
            t.material.name,
            t.storageLocation.id,
            t.storageLocation.name,
            t.material.unitOfMeasure,
            SUM(CASE WHEN t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.RECEIPT
                     THEN t.quantity ELSE -t.quantity END),
            t.material.minimumStock,
            SUM(CASE WHEN t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.RECEIPT
                     THEN t.quantity ELSE -t.quantity END) < t.material.minimumStock
        )
        FROM StockTransaction t
        GROUP BY t.material.id, t.material.materialCode, t.material.name,
                 t.storageLocation.id, t.storageLocation.name,
                 t.material.unitOfMeasure, t.material.minimumStock
        ORDER BY t.material.materialCode
        """)
    List<StockBalanceDto> getAllBalances();

    /**
     * Balance for ONE material (GET /api/stock/balance/{materialId})
     */
    @Query("""
        SELECT new vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockBalanceDto(
            t.material.id,
            t.material.materialCode,
            t.material.name,
            t.storageLocation.id,
            t.storageLocation.name,
            t.material.unitOfMeasure,
            SUM(CASE WHEN t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.RECEIPT
                     THEN t.quantity ELSE -t.quantity END),
            t.material.minimumStock,
            SUM(CASE WHEN t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.RECEIPT
                     THEN t.quantity ELSE -t.quantity END) < t.material.minimumStock
        )
        FROM StockTransaction t
        WHERE t.material.id = :materialId
        GROUP BY t.material.id, t.material.materialCode, t.material.name,
                 t.storageLocation.id, t.storageLocation.name,
                 t.material.unitOfMeasure, t.material.minimumStock
        """)
    List<StockBalanceDto> getBalanceByMaterial(@Param("materialId") UUID materialId);

    // ── CS-04: Department usage ───────────────────────────────────────────

    /**
     * Department-wise consumption summary (GET /api/stock/usage)
     * Date range is optional — pass null to get all time.
     */
    @Query("""
        SELECT new vn.edu.hust.soict.soe.assetmanagement.stock.dto.DepartmentUsageDto(
            t.requestingDepartmentId,
            t.material.id,
            t.material.materialCode,
            t.material.name,
            t.material.unitOfMeasure,
            SUM(t.quantity),
            SUM(t.totalValue)
        )
        FROM StockTransaction t
        WHERE t.transactionType = vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType.ISSUE
          AND (:startDate IS NULL OR t.documentDate >= :startDate)
          AND (:endDate   IS NULL OR t.documentDate <= :endDate)
        GROUP BY t.requestingDepartmentId, t.material.id,
                 t.material.materialCode, t.material.name, t.material.unitOfMeasure
        ORDER BY t.requestingDepartmentId, t.material.materialCode
        """)
    List<DepartmentUsageDto> getDepartmentUsage(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);
}
