package com.arthas.cataloger.service;

import com.arthas.cataloger.model.Item;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SpreadsheetService {

    private static final String[] HEADERS = {
        "ID", "Nome", "Categoria", "Descrição", "Condição",
        "Data de Aquisição", "Valor (R$)", "Observações"
    };

    public void exportToXlsx(List<Item> items, File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Coleção");

            createHeaderRow(workbook, sheet);

            for (int i = 0; i < items.size(); i++) {
                writeItemRow(sheet.createRow(i + 1), items.get(i));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    public List<Item> importFromXlsx(File file) throws IOException {
        List<Item> items = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            // linha 0 é o cabeçalho, começa a leitura da linha 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Item item = parseRow(row);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeItemRow(Row row, Item item) {
        row.createCell(0).setCellValue(item.getId());
        row.createCell(1).setCellValue(nullSafe(item.getName()));
        row.createCell(2).setCellValue(nullSafe(item.getCategory()));
        row.createCell(3).setCellValue(nullSafe(item.getDescription()));
        row.createCell(4).setCellValue(nullSafe(item.getCondition()));
        row.createCell(5).setCellValue(item.getAcquisitionDate() != null ? item.getAcquisitionDate().toString() : "");
        row.createCell(6).setCellValue(item.getValue());
        row.createCell(7).setCellValue(nullSafe(item.getNotes()));
    }

    private Item parseRow(Row row) {
        String name = getCellString(row, 1);
        if (name.isBlank()) return null;

        Item item = new Item();
        item.setName(name);
        item.setCategory(getCellString(row, 2));
        item.setDescription(getCellString(row, 3));
        item.setCondition(getCellString(row, 4));

        String dateStr = getCellString(row, 5);
        if (!dateStr.isBlank()) {
            try {
                item.setAcquisitionDate(LocalDate.parse(dateStr));
            } catch (Exception ignored) {}
        }

        Cell valueCell = row.getCell(6);
        if (valueCell != null && valueCell.getCellType() == CellType.NUMERIC) {
            item.setValue(valueCell.getNumericCellValue());
        }

        item.setNotes(getCellString(row, 7));
        return item;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
