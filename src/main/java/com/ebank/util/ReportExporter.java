package com.ebank.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ReportExporter {

    public static byte[] exportToPdf(String title, List<String> headers, List<List<String>> rows) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        // Title font
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(20);
        document.add(titlePara);

        // Table
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);

        // Header styling
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String header : headers) {
            PdfPCell hcell = new PdfPCell(new Phrase(header, headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hcell.setBackgroundColor(new Color(30, 58, 138)); // Corporate Navy
            hcell.setPadding(6);
            table.addCell(hcell);
        }

        // Data rows
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        boolean alternate = false;
        for (List<String> row : rows) {
            Color rowBg = alternate ? new Color(241, 245, 249) : Color.WHITE;
            for (String cellValue : row) {
                PdfPCell cell = new PdfPCell(new Phrase(cellValue != null ? cellValue : "", cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(rowBg);
                cell.setPadding(5);
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public static byte[] exportToExcel(String sheetName, String title, List<String> headers, List<List<String>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Title Row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header Row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(2);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 3;
            for (List<String> rowData : rows) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                for (int col = 0; col < rowData.size(); col++) {
                    row.createCell(col).setCellValue(rowData.get(col) != null ? rowData.get(col) : "");
                }
            }

            // Autosize columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
