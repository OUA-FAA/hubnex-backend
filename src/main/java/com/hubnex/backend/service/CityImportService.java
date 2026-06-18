package com.hubnex.backend.service;

import com.hubnex.backend.dto.imports.CityExcelRowDto;
import com.hubnex.backend.dto.imports.CityImportErrorDto;
import com.hubnex.backend.dto.imports.CityImportResultDto;
import com.hubnex.backend.model.City;
import com.hubnex.backend.repository.CityRepository;
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
public class CityImportService {

    private final CityRepository cityRepository;

    public CityImportResultDto importCitiesFromExcel(MultipartFile file) {
        validateFile(file);

        List<CityImportErrorDto> errors = new ArrayList<>();
        Set<String> codesInFile = new HashSet<>();
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
                CityExcelRowDto excelRow;
                try {
                    excelRow = readRow(row, formatter);
                } catch (IllegalArgumentException ex) {
                    errors.add(CityImportErrorDto.builder()
                            .row(row.getRowNum() + 1)
                            .message(ex.getMessage())
                            .build());
                    continue;
                }

                List<String> rowErrors = validateRow(excelRow, codesInFile);
                if (!rowErrors.isEmpty()) {
                    rowErrors.forEach(message -> errors.add(CityImportErrorDto.builder()
                            .row(excelRow.getRowNumber())
                            .message(message)
                            .build()));
                    continue;
                }

                codesInFile.add(normalizeCodeKey(excelRow.getCode()));
                cityRepository.save(City.builder()
                        .name(excelRow.getName())
                        .code(excelRow.getCode())
                        .active(excelRow.getActive() != null ? excelRow.getActive() : true)
                        .latitude(excelRow.getLatitude())
                        .longitude(excelRow.getLongitude())
                        .agence(null)
                        .build());
                createdCount++;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read Excel file", ex);
        }

        int skippedCount = totalRows - createdCount;
        return CityImportResultDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .skippedCount(skippedCount)
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

    private CityExcelRowDto readRow(Row row, DataFormatter formatter) {
        String name = normalizeText(formatter.formatCellValue(row.getCell(0)));
        String code = normalizeText(formatter.formatCellValue(row.getCell(1)));
        String activeText = normalizeText(formatter.formatCellValue(row.getCell(2)));
        String latitudeText = normalizeText(formatter.formatCellValue(row.getCell(3)));
        String longitudeText = normalizeText(formatter.formatCellValue(row.getCell(4)));

        return CityExcelRowDto.builder()
                .rowNumber(row.getRowNum() + 1)
                .name(name)
                .code(code)
                .active(parseActive(activeText))
                .latitude(parseCoordinate(latitudeText, "Latitude"))
                .longitude(parseCoordinate(longitudeText, "Longitude"))
                .build();
    }

    private List<String> validateRow(CityExcelRowDto row, Set<String> codesInFile) {
        List<String> errors = new ArrayList<>();

        if (row.getName() == null || row.getName().isBlank()) {
            errors.add("Name obligatoire");
        }
        if (row.getCode() == null || row.getCode().isBlank()) {
            errors.add("Code obligatoire");
        } else {
            String codeKey = normalizeCodeKey(row.getCode());
            if (codesInFile.contains(codeKey)) {
                errors.add("Code duplique dans le fichier : " + row.getCode());
            }
            if (cityRepository.existsByCode(row.getCode())) {
                errors.add("Code deja utilise : " + row.getCode());
            }
        }
        validateCoordinatePair(row, errors);

        return errors;
    }

    private Boolean parseActive(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "oui", "yes" -> true;
            case "false", "0", "non", "no" -> false;
            default -> throw new IllegalArgumentException("Valeur active invalide : " + value);
        };
    }

    private Double parseCoordinate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " doit etre numerique");
        }
    }

    private void validateCoordinatePair(CityExcelRowDto row, List<String> errors) {
        Double latitude = row.getLatitude();
        Double longitude = row.getLongitude();

        if (latitude != null && longitude == null) {
            errors.add("Longitude obligatoire si latitude est fournie");
        }
        if (longitude != null && latitude == null) {
            errors.add("Latitude obligatoire si longitude est fournie");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            errors.add("Latitude doit etre entre -90 et 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            errors.add("Longitude doit etre entre -180 et 180");
        }
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

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCodeKey(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }
}
