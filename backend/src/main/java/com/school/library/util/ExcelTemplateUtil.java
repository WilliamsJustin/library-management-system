package com.school.library.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 导入模板生成：按表头 + 示例行现场生成一份 xlsx */
public final class ExcelTemplateUtil {

    private ExcelTemplateUtil() {
    }

    /**
     * 生成导入模板 xlsx 字节流。
     *
     * @param headers  表头（加粗 + 灰底）
     * @param rows     示例数据行
     * @param colWidth 各列宽度（字符数），与表头一一对应
     */
    public static byte[] build(List<String> headers, List<String[]> rows, int[] colWidth) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("导入模板");

            XSSFCellStyle headStyle = workbook.createCellStyle();
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = head.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(c, (colWidth[c] > 0 ? colWidth[c] : 16) * 256);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = sheet.createRow(r + 1);
                String[] values = rows.get(r);
                for (int c = 0; c < values.length; c++) {
                    dataRow.createCell(c).setCellValue(values[c]);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成导入模板失败", e);
        }
    }

    /** 把模板字节包装成 xlsx 下载响应（中文文件名按 RFC 5987 编码） */
    public static ResponseEntity<byte[]> toResponse(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
