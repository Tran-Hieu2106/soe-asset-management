package vn.edu.hust.soict.soe.assetmanagement.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetReportDto {
    private UUID assetId;
    private String assetCode;
    private String assetName;
    private String categoryName;
    private String managingUnitName;
    private String status;
    private LocalDate acquisitionDate;
    private BigDecimal originalCost;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netBookValue;
}
