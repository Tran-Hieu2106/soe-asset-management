package vn.edu.hust.soict.soe.assetmanagement.report.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;
import java.math.BigDecimal;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.StockReportDto;


@Service
public class ReportMapperService {

    /**
     * Maps a Material entity and its related stock information to a StockReportDto for reporting purposes.
     * This method can be enhanced to include additional fields or perform
     * any necessary transformations specific to the report format.
     *
     * @param material The Material entity to be mapped.
     * @param openingBalance The opening balance of the material for the reporting period.
     * @param totalReceived The total quantity of the material received during the reporting period.
     * @param totalIssued The total quantity of the material issued during the reporting period.
     * @param closingBalance The closing balance of the material for the reporting period.
     * @return A StockReportDto containing the relevant information for reporting.
     */
    public StockReportDto toStockReportDto(
            Material material, 
            BigDecimal openingBalance, 
            BigDecimal totalReceived, 
            BigDecimal totalIssued, 
            BigDecimal closingBalance) {
            
        if (material == null) {
            return null;
        }

        return StockReportDto.builder()
                .materialId(material.getId())
                .materialCode(material.getMaterialCode())
                .materialName(material.getName())
                .unitOfMeasure(material.getUnitOfMeasure())
                .openingBalance(openingBalance)
                .totalReceived(totalReceived)
                .totalIssued(totalIssued)
                .closingBalance(closingBalance)
                .build();
    }


    /**
     * Maps a FixedAsset entity to an AssetReportDto for reporting purposes.
     * This method can be enhanced to include additional fields or perform
     * any necessary transformations specific to the report format.
     *
     * @param asset The FixedAsset entity to be mapped.
     * @param categoryName The name of the asset category (for better readability in reports).
     * @param unitName The name of the managing unit (for better readability in reports).
     * @return An AssetReportDto containing the relevant information for reporting.
     */
    public AssetReportDto toAssetReportDto(FixedAsset asset, String categoryName, String unitName) {
        if (asset == null) {
            return null;
        }

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