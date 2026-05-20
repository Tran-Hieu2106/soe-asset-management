package vn.edu.hust.soict.soe.assetmanagement.report.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for RP-01: Full Asset Register Report.
 * Flattens the asset data for tabular reporting.
 */
@Getter
@Builder
public class AssetReportDto {
    private UUID assetId;
    private String assetCode;
    private String assetName;
    private String status;
    private String managingUnitId;
    private LocalDate acquisitionDate;
    private BigDecimal originalCost;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netBookValue;

    public static AssetReportDto from(FixedAsset asset) {
        return AssetReportDto.builder()
                .assetId(asset.getId())
                .assetCode(asset.getAssetCode())
                .assetName(asset.getName())
                .status(asset.getStatus().name())
                .managingUnitId(asset.getManagingUnitId() != null ? asset.getManagingUnitId().toString() : "N/A")
                .acquisitionDate(asset.getAcquisitionDate())
                .originalCost(asset.getOriginalCost())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .netBookValue(asset.getNetBookValue())
                .build();
    }
}