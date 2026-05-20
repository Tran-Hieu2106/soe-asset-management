package vn.edu.hust.soict.soe.assetmanagement.report.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service to handle file exports (CSV/Excel format).
 * Utilizes standard Java to avoid external dependency bloat.
 */
@Service
public class ExportService {

    /**
     * Converts a list of AssetReportDto into a standard CSV byte array.
     * CSV files open natively in Excel.
     */
    public byte[] exportAssetsToCsv(List<AssetReportDto> data) {
        StringBuilder csv = new StringBuilder();
        
        // Add UTF-8 BOM so Excel reads Vietnamese characters correctly
        csv.append('\ufeff');

        // CSV Header
        csv.append("Asset Code,Asset Name,Status,Acquisition Date,Original Cost,Accumulated Depreciation,Net Book Value\n");

        // CSV Rows
        for (AssetReportDto row : data) {
            csv.append(escapeCsv(row.getAssetCode())).append(",")
               .append(escapeCsv(row.getAssetName())).append(",")
               .append(escapeCsv(row.getStatus())).append(",")
               .append(row.getAcquisitionDate() != null ? row.getAcquisitionDate().toString() : "").append(",")
               .append(row.getOriginalCost()).append(",")
               .append(row.getAccumulatedDepreciation()).append(",")
               .append(row.getNetBookValue()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // If value contains commas or quotes, wrap in quotes
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
