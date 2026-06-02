package vn.edu.hust.soict.soe.assetmanagement.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.AssetReportDto;
import vn.edu.hust.soict.soe.assetmanagement.report.dto.StockReportDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.DepartmentUsageDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportService {

    public byte[] exportAssetsToCsv(List<AssetReportDto> data) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append("Asset Code,Asset Name,Category,Managing Unit,Status,Acquisition Date,Original Cost,Accumulated Depreciation,Net Book Value\n");
        for (AssetReportDto row : data) {
            csv.append(escapeCsv(row.getAssetCode())).append(',')
               .append(escapeCsv(row.getAssetName())).append(',')
               .append(escapeCsv(row.getCategoryName())).append(',')
               .append(escapeCsv(row.getManagingUnitName())).append(',')
               .append(escapeCsv(row.getStatus())).append(',')
               .append(row.getAcquisitionDate() != null ? row.getAcquisitionDate() : "").append(',')
               .append(row.getOriginalCost()).append(',')
               .append(row.getAccumulatedDepreciation()).append(',')
               .append(row.getNetBookValue()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportAssetsToExcel(List<AssetReportDto> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Asset Register");
            Row header = sheet.createRow(0);
            String[] cols = {"Mã TS", "Tên TS", "Danh mục", "Đơn vị QL", "Trạng thái",
                    "Ngày ghi tăng", "Nguyên giá", "KH lũy kế", "Giá trị còn lại"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int r = 1;
            for (AssetReportDto item : data) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullSafe(item.getAssetCode()));
                row.createCell(1).setCellValue(nullSafe(item.getAssetName()));
                row.createCell(2).setCellValue(nullSafe(item.getCategoryName()));
                row.createCell(3).setCellValue(nullSafe(item.getManagingUnitName()));
                row.createCell(4).setCellValue(nullSafe(item.getStatus()));
                if (item.getAcquisitionDate() != null) {
                    row.createCell(5).setCellValue(item.getAcquisitionDate().toString());
                }
                if (item.getOriginalCost() != null) row.createCell(6).setCellValue(item.getOriginalCost().doubleValue());
                if (item.getAccumulatedDepreciation() != null) row.createCell(7).setCellValue(item.getAccumulatedDepreciation().doubleValue());
                if (item.getNetBookValue() != null) row.createCell(8).setCellValue(item.getNetBookValue().doubleValue());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất Excel báo cáo tài sản", e);
        }
    }

    public byte[] exportAssetsToPdf(List<AssetReportDto> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            doc.add(new Paragraph("BÁO CÁO TỒN KHO TÀI SẢN CỐ ĐỊNH", title));
            doc.add(new Paragraph(" "));
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (AssetReportDto item : data) {
                doc.add(new Paragraph(String.format("%s | %s | %s | %s | %s",
                        nullSafe(item.getAssetCode()), nullSafe(item.getAssetName()),
                        nullSafe(item.getCategoryName()), nullSafe(item.getManagingUnitName()),
                        nullSafe(item.getStatus())), body));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất PDF báo cáo tài sản", e);
        }
    }

    public byte[] exportStockUsageToExcel(List<DepartmentUsageDto> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Stock Usage");
            Row header = sheet.createRow(0);
            String[] cols = {"Mã VT", "Tên VT", "ĐVT", "SL xuất", "Giá trị"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int r = 1;
            for (DepartmentUsageDto item : data) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullSafe(item.getMaterialCode()));
                row.createCell(1).setCellValue(nullSafe(item.getMaterialName()));
                row.createCell(2).setCellValue(nullSafe(item.getUnitOfMeasure()));
                if (item.getTotalIssued() != null) row.createCell(3).setCellValue(item.getTotalIssued().doubleValue());
                if (item.getTotalValue() != null) row.createCell(4).setCellValue(item.getTotalValue().doubleValue());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất Excel báo cáo vật tư", e);
        }
    }

    public byte[] exportStockReportToExcel(List<StockReportDto> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Stock Balance");
            Row header = sheet.createRow(0);
            String[] cols = {"Mã VT", "Tên VT", "ĐVT", "Tồn đầu", "Nhập", "Xuất", "Tồn cuối"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int r = 1;
            for (StockReportDto item : data) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullSafe(item.getMaterialCode()));
                row.createCell(1).setCellValue(nullSafe(item.getMaterialName()));
                row.createCell(2).setCellValue(nullSafe(item.getUnitOfMeasure()));
                setDecimal(row, 3, item.getOpeningBalance());
                setDecimal(row, 4, item.getTotalReceived());
                setDecimal(row, 5, item.getTotalIssued());
                setDecimal(row, 6, item.getClosingBalance());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất Excel báo cáo tồn kho", e);
        }
    }

    private void setDecimal(Row row, int col, java.math.BigDecimal value) {
        if (value != null) row.createCell(col).setCellValue(value.doubleValue());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
