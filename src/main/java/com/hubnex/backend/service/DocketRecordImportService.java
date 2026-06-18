package com.hubnex.backend.service;

import com.hubnex.backend.dto.imports.DocketRecordImportErrorDto;
import com.hubnex.backend.dto.imports.DocketRecordImportFailedRowDto;
import com.hubnex.backend.dto.imports.DocketRecordImportResultDto;
import com.hubnex.backend.exception.DocketRecordImportException;
import com.hubnex.backend.model.DocketRecord;
import com.hubnex.backend.model.EtatColis;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.model.TypeFlux;
import com.hubnex.backend.repository.DocketRecordRepository;
import com.hubnex.backend.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocketRecordImportService {

    private static final String INVALID_EXCEL_MESSAGE =
            "Le fichier est vide, illisible ou n'est pas un fichier Excel valide.";

    private static final List<String> REQUIRED_HEADERS = List.of(
            "Batch Number",
            "LTA",
            "Connote",
            "Hub",
            "Line",
            "weight (Kg)",
            "Width",
            "Length",
            "Height",
            "volume (L*L*H/5000)",
            "Date reception"
    );

    private final DocketRecordRepository docketRecordRepository;
    private final HubRepository hubRepository;
    private final TrackingService trackingService;

    @Transactional
    public DocketRecordImportResultDto importFromExcel(MultipartFile file) {
        return importFromExcel(file, null);
    }

    @Transactional
    public DocketRecordImportResultDto importFromExcel(MultipartFile file, String requestedTypeFlux) {
        return processExcel(file, requestedTypeFlux, true);
    }

    @Transactional(readOnly = true)
    public DocketRecordImportResultDto previewFromExcel(MultipartFile file) {
        return processExcel(file, null, false);
    }

    private DocketRecordImportResultDto processExcel(MultipartFile file, String requestedTypeFlux, boolean persist) {
        validateFile(file);
        TypeFlux typeFlux = persist ? resolveTypeFlux(requestedTypeFlux) : null;

        List<DocketRecordImportErrorDto> errors = new ArrayList<>();
        List<DocketRecordImportFailedRowDto> failedRows = new ArrayList<>();
        Set<String> connotesInFile = new HashSet<>();
        String importBatchId = persist ? UUID.randomUUID().toString() : null;
        String manifestName = persist ? file.getOriginalFilename() : null;
        LocalDateTime importedAt = persist ? LocalDateTime.now() : null;
        int totalRows = 0;
        int createdCount = 0;

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw invalidExcelException(null);
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = readAndValidateHeaders(sheet, formatter, errors);
            if (!errors.isEmpty()) {
                return result(0, 0, errors, failedRows);
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }

                totalRows++;
                int rowNumber = row.getRowNum() + 1;
                DocketRecordImportFailedRowDto originalRow = mapOriginalRow(rowNumber, row, columns, formatter);
                String connote = readText(row, columns, "Connote", formatter);
                String hubName = readText(row, columns, "Hub", formatter);

                validateConnote(rowNumber, connote, connotesInFile, errors);
                Hub hub = resolveHub(rowNumber, hubName, errors);
                Double poids = parseDouble(row, columns, "weight (Kg)", formatter, errors);
                Double largeur = parseDouble(row, columns, "Width", formatter, errors);
                Double longueur = parseDouble(row, columns, "Length", formatter, errors);
                Double hauteur = parseDouble(row, columns, "Height", formatter, errors);
                Double volume = parseDouble(row, columns, "volume (L*L*H/5000)", formatter, errors);
                LocalDateTime dateReception = parseDate(row, columns, "Date reception", formatter, errors);

                if (hasRowErrors(errors, rowNumber)) {
                    addFailedRows(originalRow, errorsForRow(errors, rowNumber), failedRows);
                    continue;
                }

                if (persist) {
                    DocketRecord record = DocketRecord.builder()
                            .numeroBatch(readText(row, columns, "Batch Number", formatter))
                            .lta(readText(row, columns, "LTA", formatter))
                            .connote(connote)
                            .manifestName(manifestName)
                            .importBatchId(importBatchId)
                            .importedAt(importedAt)
                            .typeFlux(typeFlux)
                            .hubPrincipal(hub)
                            .ligne(readText(row, columns, "Line", formatter))
                            .poids(poids)
                            .largeur(largeur)
                            .longueur(longueur)
                            .hauteur(hauteur)
                            .poidsVolumetrique(resolvePoidsVolumetrique(volume, largeur, longueur, hauteur))
                            .dateReception(dateReception)
                            .etat(dateReception != null ? EtatColis.ARRIVE_HUB : EtatColis.CREE)
                            .estArrive(dateReception != null)
                            .estConvoyeur(false)
                            .estDispatche(false)
                            .apiCree(false)
                            .build();

                    DocketRecord savedRecord = docketRecordRepository.save(record);
                    trackingService.createEvent(
                            savedRecord,
                            "CREE",
                            "Colis import\u00e9 dans le manifeste");
                }
                connotesInFile.add(normalizeKey(connote));
                createdCount++;
            }
        } catch (IOException ex) {
            throw invalidExcelException(ex);
        } catch (POIXMLException | IllegalArgumentException ex) {
            throw invalidExcelException(ex);
        }

        return result(totalRows, createdCount, errors, failedRows);
    }

    private TypeFlux resolveTypeFlux(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TypeFlux.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("typeFlux invalide. Valeurs autorisees : RECEPTION, DISPATCH, EXPEDITION");
        }
    }

    public byte[] exportFailedRows(List<DocketRecordImportFailedRowDto> failedRows) {
        List<DocketRecordImportFailedRowDto> rows = failedRows != null ? failedRows : List.of();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Failed manifeste rows");
            List<String> headers = List.of(
                    "Row Number",
                    "Batch Number",
                    "LTA",
                    "Connote",
                    "Hub",
                    "Line",
                    "weight (Kg)",
                    "Width",
                    "Length",
                    "Height",
                    "volume (L*L*H/5000)",
                    "Date reception",
                    "Error Field",
                    "Error Message"
            );

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) {
                headerRow.createCell(index).setCellValue(headers.get(index));
            }

            for (int index = 0; index < rows.size(); index++) {
                DocketRecordImportFailedRowDto failedRow = rows.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(failedRow.getRowNumber());
                row.createCell(1).setCellValue(value(failedRow.getBatchNumber()));
                row.createCell(2).setCellValue(value(failedRow.getLta()));
                row.createCell(3).setCellValue(value(failedRow.getConnote()));
                row.createCell(4).setCellValue(value(failedRow.getHub()));
                row.createCell(5).setCellValue(value(failedRow.getLine()));
                row.createCell(6).setCellValue(value(failedRow.getWeight()));
                row.createCell(7).setCellValue(value(failedRow.getWidth()));
                row.createCell(8).setCellValue(value(failedRow.getLength()));
                row.createCell(9).setCellValue(value(failedRow.getHeight()));
                row.createCell(10).setCellValue(value(failedRow.getVolume()));
                row.createCell(11).setCellValue(value(failedRow.getDateReception()));
                row.createCell(12).setCellValue(value(failedRow.getErrorField()));
                row.createCell(13).setCellValue(value(failedRow.getErrorMessage()));
            }

            for (int index = 0; index < headers.size(); index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Could not export failed rows", ex);
        }
    }

    private Map<String, Integer> readAndValidateHeaders(Sheet sheet, DataFormatter formatter,
                                                        List<DocketRecordImportErrorDto> errors) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            errors.add(error(1, "header", "Ligne header obligatoire"));
            return Map.of();
        }

        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (!header.isBlank()) {
                columns.put(header, cell.getColumnIndex());
            }
        }

        for (String header : REQUIRED_HEADERS) {
            if (!columns.containsKey(normalizeHeader(header))) {
                errors.add(error(1, header, "Colonne obligatoire manquante : " + header));
            }
        }
        return columns;
    }

    private void validateConnote(int rowNumber, String connote, Set<String> connotesInFile,
                                 List<DocketRecordImportErrorDto> errors) {
        if (connote == null || connote.isBlank()) {
            errors.add(error(rowNumber, "Connote", "Connote obligatoire"));
            return;
        }

        String key = normalizeKey(connote);
        if (connotesInFile.contains(key)) {
            errors.add(error(rowNumber, "Connote", "Connote dupliquee dans le fichier"));
        }
        if (docketRecordRepository.existsByConnote(connote)) {
            errors.add(error(rowNumber, "Connote", "Connote deja existante"));
        }
    }

    private Hub resolveHub(int rowNumber, String hubName, List<DocketRecordImportErrorDto> errors) {
        if (hubName == null || hubName.isBlank()) {
            errors.add(error(rowNumber, "Hub", "Hub obligatoire"));
            return null;
        }

        return hubRepository.findByNomIgnoreCase(hubName)
                .orElseGet(() -> {
                    errors.add(error(rowNumber, "Hub", "Hub introuvable : " + hubName));
                    return null;
                });
    }

    private Double resolvePoidsVolumetrique(Double volume, Double largeur, Double longueur, Double hauteur) {
        if (volume != null) {
            return volume;
        }
        if (largeur != null && longueur != null && hauteur != null) {
            return (largeur * longueur * hauteur) / 5000;
        }
        return null;
    }

    private String readText(Row row, Map<String, Integer> columns, String header, DataFormatter formatter) {
        Integer index = columns.get(normalizeHeader(header));
        if (index == null || row == null) {
            return "";
        }
        return normalizeText(formatter.formatCellValue(row.getCell(index)));
    }

    private Double parseDouble(Row row, Map<String, Integer> columns, String header, DataFormatter formatter,
                               List<DocketRecordImportErrorDto> errors) {
        Integer index = columns.get(normalizeHeader(header));
        if (index == null || row == null) {
            return null;
        }
        String value = normalizeText(formatter.formatCellValue(row.getCell(index)));
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            errors.add(error(row.getRowNum() + 1, header, "Valeur numerique invalide"));
            return null;
        }
    }

    private LocalDateTime parseDate(Row row, Map<String, Integer> columns, String header, DataFormatter formatter,
                                    List<DocketRecordImportErrorDto> errors) {
        Integer index = columns.get(normalizeHeader(header));
        if (index == null || row == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        String value = normalizeText(formatter.formatCellValue(cell));
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            errors.add(error(row.getRowNum() + 1, header, "Date reception invalide"));
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidExcelException(null);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw invalidExcelException(null);
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            throw invalidExcelException(null);
        }
    }

    private Workbook openWorkbook(MultipartFile file) {
        try {
            return WorkbookFactory.create(file.getInputStream());
        } catch (IOException | RuntimeException ex) {
            throw invalidExcelException(ex);
        }
    }

    private DocketRecordImportException invalidExcelException(Throwable cause) {
        DocketRecordImportResultDto errorResult = result(
                0,
                0,
                List.of(error(0, "file", INVALID_EXCEL_MESSAGE)),
                List.of());

        if (cause == null) {
            return new DocketRecordImportException(INVALID_EXCEL_MESSAGE, errorResult);
        }
        return new DocketRecordImportException(INVALID_EXCEL_MESSAGE, cause, errorResult);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < REQUIRED_HEADERS.size(); index++) {
            if (!normalizeText(formatter.formatCellValue(row.getCell(index))).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasRowErrors(List<DocketRecordImportErrorDto> errors, int rowNumber) {
        return errors.stream().anyMatch(error -> error.getRowNumber() == rowNumber);
    }

    private List<DocketRecordImportErrorDto> errorsForRow(List<DocketRecordImportErrorDto> errors, int rowNumber) {
        return errors.stream()
                .filter(error -> error.getRowNumber() == rowNumber)
                .toList();
    }

    private void addFailedRows(DocketRecordImportFailedRowDto originalRow, List<DocketRecordImportErrorDto> rowErrors,
                               List<DocketRecordImportFailedRowDto> failedRows) {
        rowErrors.forEach(rowError -> failedRows.add(DocketRecordImportFailedRowDto.builder()
                .rowNumber(originalRow.getRowNumber())
                .batchNumber(originalRow.getBatchNumber())
                .lta(originalRow.getLta())
                .connote(originalRow.getConnote())
                .hub(originalRow.getHub())
                .line(originalRow.getLine())
                .weight(originalRow.getWeight())
                .width(originalRow.getWidth())
                .length(originalRow.getLength())
                .height(originalRow.getHeight())
                .volume(originalRow.getVolume())
                .dateReception(originalRow.getDateReception())
                .errorField(rowError.getField())
                .errorMessage(rowError.getMessage())
                .build()));
    }

    private DocketRecordImportFailedRowDto mapOriginalRow(int rowNumber, Row row, Map<String, Integer> columns,
                                                          DataFormatter formatter) {
        return DocketRecordImportFailedRowDto.builder()
                .rowNumber(rowNumber)
                .batchNumber(readText(row, columns, "Batch Number", formatter))
                .lta(readText(row, columns, "LTA", formatter))
                .connote(readText(row, columns, "Connote", formatter))
                .hub(readText(row, columns, "Hub", formatter))
                .line(readText(row, columns, "Line", formatter))
                .weight(readText(row, columns, "weight (Kg)", formatter))
                .width(readText(row, columns, "Width", formatter))
                .length(readText(row, columns, "Length", formatter))
                .height(readText(row, columns, "Height", formatter))
                .volume(readText(row, columns, "volume (L*L*H/5000)", formatter))
                .dateReception(readText(row, columns, "Date reception", formatter))
                .build();
    }

    private DocketRecordImportResultDto result(int totalRows, int createdCount, List<DocketRecordImportErrorDto> errors,
                                               List<DocketRecordImportFailedRowDto> failedRows) {
        return DocketRecordImportResultDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .skippedCount(totalRows - createdCount)
                .errors(errors != null ? errors : List.of())
                .failedRows(failedRows != null ? failedRows : List.of())
                .build();
    }

    private DocketRecordImportErrorDto error(int rowNumber, String field, String message) {
        return DocketRecordImportErrorDto.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeHeader(String value) {
        return normalizeText(value)
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        return normalizeText(value).toLowerCase(Locale.ROOT);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
