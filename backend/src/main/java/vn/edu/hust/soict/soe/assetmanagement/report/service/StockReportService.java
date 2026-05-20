package vn.edu.hust.soict.soe.assetmanagement.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.StockReportDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StockTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating Stock Reports (RP-02).
 * Integrates with Stock (M3) to calculate period balances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReportService {

    private final MaterialRepository materialRepository;
    private final StockTransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<StockReportDto> generateStockReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating Stock Report from {} to {}", startDate, endDate);

        List<Material> materials = materialRepository.findAll();
        
        List<StockReportDto> report = materials.stream().map(material -> {
            // Fetch all transactions for this material
            List<StockTransaction> allTx = transactionRepository.findByMaterialIdOrderByDocumentDateDesc(material.getId());

            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal receivedPeriod = BigDecimal.ZERO;
            BigDecimal issuedPeriod = BigDecimal.ZERO;

            for (StockTransaction tx : allTx) {
                boolean isBeforeStart = startDate != null && tx.getDocumentDate().isBefore(startDate);
                boolean isWithinPeriod = (startDate == null || !tx.getDocumentDate().isBefore(startDate)) &&
                                         (endDate == null || !tx.getDocumentDate().isAfter(endDate));

                BigDecimal qty = tx.getQuantity();

                if (tx.getTransactionType() == TransactionType.RECEIPT) {
                    if (isBeforeStart) openingBalance = openingBalance.add(qty);
                    if (isWithinPeriod) receivedPeriod = receivedPeriod.add(qty);
                } else if (tx.getTransactionType() == TransactionType.ISSUE) {
                    if (isBeforeStart) openingBalance = openingBalance.subtract(qty);
                    if (isWithinPeriod) issuedPeriod = issuedPeriod.add(qty);
                }
            }

            BigDecimal closingBalance = openingBalance.add(receivedPeriod).subtract(issuedPeriod);

            return StockReportDto.builder()
                    .materialId(material.getId())
                    .materialCode(material.getMaterialCode())
                    .materialName(material.getName())
                    .unitOfMeasure(material.getUnitOfMeasure())
                    .openingBalance(openingBalance)
                    .totalReceived(receivedPeriod)
                    .totalIssued(issuedPeriod)
                    .closingBalance(closingBalance)
                    .build();

        }).collect(Collectors.toList());

        auditLogService.log("REPORT", "GENERATE_STOCK_REPORT", null, null, 
            "{}", "{\"startDate\": \"" + startDate + "\", \"endDate\": \"" + endDate + "\"}", "Generated stock balance report");

        return report;
    }
}