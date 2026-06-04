package vn.edu.hust.soict.soe.assetmanagement.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for RP-02: Stock Balance and Usage Report.
 * Represents a snapshot of material movements over a date range.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReportDto {
    private UUID materialId;
    private String materialCode;
    private String materialName;
    private String unitOfMeasure;
    
    // Aggregated metrics for the reporting period
    private BigDecimal openingBalance;
    private BigDecimal totalReceived;
    private BigDecimal totalIssued;
    private BigDecimal closingBalance;
}