package com.hubnex.backend.service;

import com.hubnex.backend.dto.imports.HubImportErrorDto;
import com.hubnex.backend.dto.imports.HubImportResultDto;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HubImportService {

    private final HubRepository hubRepository;

    public HubImportResultDto importHubsFromExcel(MultipartFile file) {
        validateFile(file);

        List<HubImportErrorDto> errors = new ArrayList<>();
        Set<String> nomsInFile = new HashSet<>();
        Set<String> barcodesInFile = new HashSet<>();
        int totalRows = 0;
        int createdCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new RuntimeException("Excel file does not contain any sheet");
            }

            DataFormatter formatter = new DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }

                totalRows++;
                int rowNumber = row.getRowNum() + 1;
                String nom = normalizeText(formatter.formatCellValue(row.getCell(0)));
                String telephone = normalizeText(formatter.formatCellValue(row.getCell(1)));
                String adresse = normalizeText(formatter.formatCellValue(row.getCell(2)));
                String barcode = normalizeText(formatter.formatCellValue(row.getCell(3)));
                String activeText = normalizeText(formatter.formatCellValue(row.getCell(4)));

                Boolean actif = null;
                List<HubImportErrorDto> rowErrors = new ArrayList<>();
                try {
                    actif = parseActive(activeText);
                } catch (IllegalArgumentException ex) {
                    rowErrors.add(error(rowNumber, "Active", ex.getMessage()));
                }

                validateRow(rowNumber, nom, barcode, nomsInFile, barcodesInFile, rowErrors);
                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }

                nomsInFile.add(normalizeKey(nom));
                if (!barcode.isBlank()) {
                    barcodesInFile.add(normalizeKey(barcode));
                }

                hubRepository.save(Hub.builder()
                        .nom(nom)
                        .telephone(emptyToNull(telephone))
                        .adresse(emptyToNull(adresse))
                        .barcode(emptyToNull(barcode))
                        .actif(actif != null ? actif : true)
                        .build());
                createdCount++;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read Excel file", ex);
        }

        return HubImportResultDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .skippedCount(totalRows - createdCount)
                .errors(errors)
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Excel file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new RuntimeException("Excel filename is required");
        }

        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            throw new RuntimeException("Only .xlsx and .xls files are allowed");
        }
    }

    private void validateRow(int rowNumber,
                             String nom,
                             String barcode,
                             Set<String> nomsInFile,
                             Set<String> barcodesInFile,
                             List<HubImportErrorDto> errors) {
        if (nom == null || nom.isBlank()) {
            errors.add(error(rowNumber, "Nom", "Nom is required"));
        } else {
            String nomKey = normalizeKey(nom);
            if (nomsInFile.contains(nomKey)) {
                errors.add(error(rowNumber, "Nom", "Nom duplicated in file: " + nom));
            }
            if (hubRepository.existsByNom(nom)) {
                errors.add(error(rowNumber, "Nom", "Nom already exists: " + nom));
            }
        }

        if (barcode != null && !barcode.isBlank()) {
            String barcodeKey = normalizeKey(barcode);
            if (barcodesInFile.contains(barcodeKey)) {
                errors.add(error(rowNumber, "Barcode", "Barcode duplicated in file: " + barcode));
            }
            if (hubRepository.existsByBarcode(barcode)) {
                errors.add(error(rowNumber, "Barcode", "Barcode already exists: " + barcode));
            }
        }
    }

    private Boolean parseActive(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "oui", "yes" -> true;
            case "false", "0", "non", "no" -> false;
            default -> throw new IllegalArgumentException("Active must be TRUE/FALSE, 1/0 or Oui/Non");
        };
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        return normalizeText(formatter.formatCellValue(row.getCell(0))).isBlank()
                && normalizeText(formatter.formatCellValue(row.getCell(1))).isBlank()
                && normalizeText(formatter.formatCellValue(row.getCell(2))).isBlank()
                && normalizeText(formatter.formatCellValue(row.getCell(3))).isBlank()
                && normalizeText(formatter.formatCellValue(row.getCell(4))).isBlank();
    }

    private HubImportErrorDto error(int rowNumber, String field, String message) {
        return HubImportErrorDto.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
