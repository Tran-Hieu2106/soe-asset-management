package vn.edu.hust.soict.soe.assetmanagement.report.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
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

    public static AssetReportDto from(FixedAsset asset, String categoryName, String unitName) {
        return AssetReportDto.builder()
                .assetId(asset.getId())
                .assetCode(asset.getAssetCode())
                .assetName(asset.getName())
                .categoryName(categoryName)
                .managingUnitName(unitName)
                .status(asset.getStatus().name())
                .acquisitionDate(asset.getAcquisitionDate())
                .originalCost(asset.getOriginalCost())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .netBookValue(asset.getNetBookValue())
                .build();
    }
}
