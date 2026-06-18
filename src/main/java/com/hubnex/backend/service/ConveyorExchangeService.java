package com.hubnex.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubnex.backend.dto.conveyor.ConveyorExchangeResultDto;
import com.hubnex.backend.dto.conveyor.ConveyorParcelDto;
import com.hubnex.backend.dto.imports.DocketRecordImportErrorDto;
import com.hubnex.backend.dto.imports.DocketRecordUpdateResultDto;
import com.hubnex.backend.dto.response.DocketRecordResponseDto;
import com.hubnex.backend.dto.response.ReceptionRecoveryResponseDto;
import com.hubnex.backend.model.DocketRecord;
import com.hubnex.backend.model.TypeFlux;
import com.hubnex.backend.repository.DocketRecordRepository;
import com.hubnex.backend.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConveyorExchangeService {

    private static final List<String> CORRECTION_HEADERS = List.of(
            "Connote",
            "weight (Kg)",
            "Width",
            "Length",
            "Height",
            "volume (L*L*H/5000)"
    );
    private static final DateTimeFormatter RECOVERY_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DocketRecordRepository docketRecordRepository;
    private final DocketRecordService docketRecordService;
    private final TrackingRepository trackingRepository;
    private final TrackingService trackingService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${conveyor.api.send-url:}")
    private String conveyorSendUrl;

    @Value("${conveyor.api.response-url:}")
    private String conveyorResponseUrl;

    @Transactional
    public ConveyorExchangeResultDto sendToConveyor() {
        throw badRequest("Selection des manifestes obligatoire");
    }

    @Transactional
    public ConveyorExchangeResultDto sendToConveyor(String requestedTypeFlux, String importBatchId) {
        Set<String> importBatchIds = importBatchId != null && !importBatchId.isBlank()
                ? Set.of(importBatchId)
                : Set.of();
        return sendToConveyor(requestedTypeFlux, importBatchIds);
    }

    @Transactional
    public ConveyorExchangeResultDto sendToConveyor(TypeFlux requestedTypeFlux, Set<String> importBatchIds) {
        return sendToConveyor(requestedTypeFlux != null ? requestedTypeFlux.name() : null, importBatchIds);
    }

    @Transactional
    public ConveyorExchangeResultDto sendToConveyor(String requestedTypeFlux, Set<String> importBatchIds) {
        TypeFlux typeFlux = resolveRequiredTypeFlux(requestedTypeFlux);
        List<DocketRecord> selectedRecords = resolveRecordsForConveyor(importBatchIds);
        String conveyorSendBatchId = UUID.randomUUID().toString();
        LocalDateTime conveyorSentAt = LocalDateTime.now();
        selectedRecords.forEach(record -> {
            record.setTypeFlux(typeFlux);
            if (typeFlux == TypeFlux.EXPEDITION && record.getEtat() == null) {
                record.setEtat(com.hubnex.backend.model.EtatColis.EN_LIVRAISON);
            }
        });

        List<DocketRecord> records = selectedRecords.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getConveyorSent()))
                .toList();
        List<ConveyorParcelDto> payload = records.stream()
                .map(this::mapToConveyorParcel)
                .toList();

        if (isConfigured(conveyorSendUrl)) {
            restTemplate.postForEntity(conveyorSendUrl, new HttpEntity<>(payload), String.class);
        }

        records.forEach(record -> {
            record.setEstConvoyeur(true);
            record.setConveyorSent(true);
            record.setConveyorSentAt(conveyorSentAt);
            record.setConveyorSendBatchId(conveyorSendBatchId);
        });
        docketRecordRepository.saveAll(selectedRecords);
        records.forEach(record -> trackingService.createEvent(
                record,
                "ENVOYE_CONVOYEUR",
                "Colis envoy\u00e9 au convoyeur",
                typeFlux));

        if (shouldAutoRecover(typeFlux) && !records.isEmpty()) {
            ConveyorExchangeResultDto recoveryResult = applyConveyorResponse(typeFlux, records, autoRecoveryNamePrefix(typeFlux));
            return ConveyorExchangeResultDto.builder()
                    .message("Records sent to conveyor with batch " + conveyorSendBatchId
                            + ". " + typeFlux + " response recovered automatically.")
                    .totalRows(selectedRecords.size())
                    .sentCount(records.size())
                    .updatedCount(recoveryResult.getUpdatedCount())
                    .skippedCount(selectedRecords.size() - records.size() + recoveryResult.getSkippedCount())
                    .errors(recoveryResult.getErrors())
                    .build();
        }

        return ConveyorExchangeResultDto.builder()
                .message(records.isEmpty()
                        ? "No new records sent to conveyor for selected manifests."
                        : "Records sent to conveyor with batch " + conveyorSendBatchId + ".")
                .totalRows(selectedRecords.size())
                .sentCount(records.size())
                .updatedCount(0)
                .skippedCount(selectedRecords.size() - records.size())
                .errors(List.of())
                .build();
    }

    private boolean shouldAutoRecover(TypeFlux typeFlux) {
        return typeFlux == TypeFlux.DISPATCH || typeFlux == TypeFlux.EXPEDITION;
    }

    private String autoRecoveryNamePrefix(TypeFlux typeFlux) {
        return switch (typeFlux) {
            case DISPATCH -> "Dispatch auto recovery";
            case EXPEDITION -> "Expedition auto recovery";
            case RECEPTION -> "Reception auto recovery";
        };
    }

    @Transactional
    public ConveyorExchangeResultDto fetchAndApplyConveyorResponse() {
        return fetchAndApplyConveyorResponse(null, Set.of());
    }

    @Transactional
    public ConveyorExchangeResultDto fetchAndApplyConveyorResponse(String requestedTypeFlux) {
        return fetchAndApplyConveyorResponse(requestedTypeFlux, Set.of());
    }

    @Transactional
    public ConveyorExchangeResultDto fetchAndApplyConveyorResponse(String requestedTypeFlux, Set<String> importBatchIds) {
        TypeFlux typeFlux = resolveRequiredTypeFlux(requestedTypeFlux);
        List<DocketRecord> eligibleRecords = resolveRecordsForConveyorResponse(typeFlux, importBatchIds);
        if (eligibleRecords.isEmpty()) {
            return ConveyorExchangeResultDto.builder()
                    .message("No sent conveyor records found for typeFlux " + typeFlux + ".")
                    .totalRows(0)
                    .sentCount(0)
                    .updatedCount(0)
                    .skippedCount(0)
                    .errors(List.of())
                    .build();
        }
        return applyConveyorResponse(typeFlux, eligibleRecords, "Conveyor response");
    }

    private ConveyorExchangeResultDto applyConveyorResponse(TypeFlux typeFlux, List<DocketRecord> eligibleRecords, String recoveryNamePrefix) {
        Set<String> eligibleConnotes = eligibleRecords.stream()
                .map(DocketRecord::getConnote)
                .filter(connote -> connote != null && !connote.isBlank())
                .collect(Collectors.toSet());
        List<DocketRecordImportErrorDto> errors = new ArrayList<>();
        List<ConveyorParcelDto> parcels = isConfigured(conveyorResponseUrl)
                ? fetchConveyorParcels()
                : mockConveyorParcels(eligibleRecords);
        parcels = parcels.stream()
                .filter(parcel -> parcel.getConnote() != null && eligibleConnotes.contains(parcel.getConnote()))
                .toList();

        if (parcels.isEmpty()) {
            return ConveyorExchangeResultDto.builder()
                    .message("No conveyor records found for selected sent manifests and typeFlux " + typeFlux + ".")
                    .totalRows(0)
                    .sentCount(0)
                    .updatedCount(0)
                    .skippedCount(0)
                    .errors(List.of())
                    .build();
        }

        String receptionRecoveryId = UUID.randomUUID().toString();
        LocalDateTime recoveredAt = LocalDateTime.now();
        String recoveryName = recoveryNamePrefix + " " + recoveredAt.format(RECOVERY_NAME_FORMATTER);
        int updatedCount = 0;

        for (int index = 0; index < parcels.size(); index++) {
            int rowNumber = index + 1;
            ConveyorParcelDto parcel = parcels.get(index);
            if (parcel.getConnote() == null || parcel.getConnote().isBlank()) {
                errors.add(error(rowNumber, "connote", "Connote obligatoire dans la reponse convoyeur"));
                continue;
            }

            DocketRecord record = docketRecordRepository.findByConnote(parcel.getConnote())
                    .orElse(null);
            if (record == null) {
                errors.add(error(rowNumber, "connote", "DocketRecord introuvable pour connote : " + parcel.getConnote()));
                continue;
            }
            if (record.getTypeFlux() != typeFlux) {
                errors.add(error(rowNumber, "typeFlux", "DocketRecord ignore car typeFlux different : " + parcel.getConnote()));
                continue;
            }
            if (!Boolean.TRUE.equals(record.getConveyorSent())) {
                errors.add(error(rowNumber, "conveyorSent", "DocketRecord ignore car non envoye au convoyeur : " + parcel.getConnote()));
                continue;
            }
            if (record.getRecoveredAt() != null) {
                errors.add(error(rowNumber, "recoveredAt", "DocketRecord ignore car deja recupere : " + parcel.getConnote()));
                continue;
            }
            if (!eligibleConnotes.contains(record.getConnote())) {
                errors.add(error(rowNumber, "importBatchId", "DocketRecord ignore car hors selection : " + parcel.getConnote()));
                continue;
            }

            applyMissingMeasurements(record, parcel);
            record.setEstConvoyeur(true);
            record.setReceptionRecoveryId(receptionRecoveryId);
            record.setRecoveryName(recoveryName);
            record.setRecoveredAt(recoveredAt);
            docketRecordRepository.save(record);
            createRecoveryTracking(record, typeFlux);
            updatedCount++;
        }

        return ConveyorExchangeResultDto.builder()
                .message(updatedCount > 0
                        ? "Conveyor response processed for typeFlux " + typeFlux + "."
                        : "No conveyor records found for selected sent manifests and typeFlux " + typeFlux + ".")
                .totalRows(parcels.size())
                .sentCount(0)
                .updatedCount(updatedCount)
                .skippedCount(parcels.size() - updatedCount)
                .errors(errors)
                .build();
    }

    private void createRecoveryTracking(DocketRecord record, TypeFlux typeFlux) {
        switch (typeFlux) {
            case RECEPTION -> trackingService.createEvent(
                    record,
                    "ARRIVE_HUB",
                    "Colis r\u00e9cup\u00e9r\u00e9 en r\u00e9ception");
            case DISPATCH -> trackingService.createEvent(
                    record,
                    "EN_TRANSIT",
                    "Colis r\u00e9cup\u00e9r\u00e9 en dispatch");
            case EXPEDITION -> trackingService.createEvent(
                    record,
                    "EN_LIVRAISON",
                    "Colis r\u00e9cup\u00e9r\u00e9 en exp\u00e9dition");
        }
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getConveyorReadyRecords() {
        return getConveyorReadyRecords(TypeFlux.RECEPTION);
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getConveyorReadyRecords(TypeFlux typeFlux) {
        return docketRecordRepository.findAll().stream()
                .filter(record -> record.getTypeFlux() == typeFlux)
                .filter(record -> Boolean.TRUE.equals(record.getEstConvoyeur()))
                .filter(this::isComplete)
                .map(docketRecordService::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceptionRecoveryResponseDto> getRecoveries() {
        return getRecoveries(TypeFlux.RECEPTION);
    }

    @Transactional(readOnly = true)
    public List<ReceptionRecoveryResponseDto> getRecoveries(TypeFlux typeFlux) {
        return docketRecordRepository.findByReceptionRecoveryIdIsNotNullAndTypeFlux(typeFlux).stream()
                .collect(Collectors.groupingBy(DocketRecord::getReceptionRecoveryId, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(this::mapToRecoveryResponse)
                .sorted(Comparator.comparing(ReceptionRecoveryResponseDto::getRecoveredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getRecoveryRecords(String receptionRecoveryId) {
        return getRecoveryRecords(receptionRecoveryId, TypeFlux.RECEPTION);
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getRecoveryRecords(String receptionRecoveryId, TypeFlux typeFlux) {
        return docketRecordRepository.findByReceptionRecoveryIdAndTypeFlux(receptionRecoveryId, typeFlux).stream()
                .map(docketRecordService::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteRecovery(String recoveryId, TypeFlux typeFlux) {
        if (recoveryId == null || recoveryId.isBlank()) {
            throw badRequest("recoveryId est obligatoire");
        }
        if (typeFlux == null) {
            throw badRequest("typeFlux est obligatoire");
        }

        List<DocketRecord> records = docketRecordRepository
                .findByReceptionRecoveryIdAndTypeFlux(recoveryId, typeFlux);
        if (records.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Recovery introuvable pour typeFlux " + typeFlux);
        }

        List<Long> recordIds = records.stream().map(DocketRecord::getId).toList();
        trackingRepository.deleteByDocketRecordIds(recordIds);
        docketRecordRepository.deleteAllInBatch(records);
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getIncompleteRecords() {
        return docketRecordRepository.findAll().stream()
                .filter(record -> !isComplete(record))
                .map(docketRecordService::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportIncompleteRecords() {
        List<DocketRecord> records = docketRecordRepository.findAll().stream()
                .filter(record -> !isComplete(record))
                .toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Incomplete docket records");
            Row header = sheet.createRow(0);
            for (int index = 0; index < CORRECTION_HEADERS.size(); index++) {
                header.createCell(index).setCellValue(CORRECTION_HEADERS.get(index));
            }

            for (int index = 0; index < records.size(); index++) {
                DocketRecord record = records.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(value(record.getConnote()));
                setNumericCell(row, 1, record.getPoids());
                setNumericCell(row, 2, record.getLargeur());
                setNumericCell(row, 3, record.getLongueur());
                setNumericCell(row, 4, record.getHauteur());
                setNumericCell(row, 5, record.getPoidsVolumetrique());
            }

            for (int index = 0; index < CORRECTION_HEADERS.size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Could not export incomplete docket records", ex);
        }
    }

    @Transactional
    public DocketRecordUpdateResultDto importCorrectedIncompleteRecords(MultipartFile file) {
        validateExcelFile(file);

        List<DocketRecordImportErrorDto> errors = new ArrayList<>();
        int totalRows = 0;
        int updatedCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new RuntimeException("Excel file does not contain any sheet");
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = readHeaders(sheet, formatter, errors);
            if (!errors.isEmpty()) {
                return updateResult(0, 0, errors);
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }

                totalRows++;
                int rowNumber = row.getRowNum() + 1;
                String connote = readText(row, columns, "Connote", formatter);
                if (connote.isBlank()) {
                    errors.add(error(rowNumber, "Connote", "Connote obligatoire"));
                    continue;
                }

                DocketRecord record = docketRecordRepository.findByConnote(connote).orElse(null);
                if (record == null) {
                    errors.add(error(rowNumber, "Connote", "DocketRecord introuvable : " + connote));
                    continue;
                }

                ConveyorParcelDto parcel = ConveyorParcelDto.builder()
                        .connote(connote)
                        .poids(parseDouble(row, columns, "weight (Kg)", formatter, errors))
                        .largeur(parseDouble(row, columns, "Width", formatter, errors))
                        .longueur(parseDouble(row, columns, "Length", formatter, errors))
                        .hauteur(parseDouble(row, columns, "Height", formatter, errors))
                        .poidsVolumetrique(parseDouble(row, columns, "volume (L*L*H/5000)", formatter, errors))
                        .build();

                if (hasRowErrors(errors, rowNumber)) {
                    continue;
                }

                applyMeasurements(record, parcel);
                if (isComplete(record)) {
                    record.setEstConvoyeur(true);
                }
                docketRecordRepository.save(record);
                updatedCount++;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read Excel file", ex);
        }

        return updateResult(totalRows, updatedCount, errors);
    }

    private List<ConveyorParcelDto> fetchConveyorParcels() {
        try {
            String response = restTemplate.getForObject(conveyorResponseUrl, String.class);
            if (response == null || response.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode rows = root.isArray() ? root : firstExisting(root, "records", "data", "items", "docketRecords");
            if (rows == null || !rows.isArray()) {
                throw new RuntimeException("Conveyor response must be an array or contain records/data/items");
            }

            List<ConveyorParcelDto> parcels = new ArrayList<>();
            rows.forEach(node -> parcels.add(ConveyorParcelDto.builder()
                    .connote(text(node, "connote", "Connote"))
                    .poids(number(node, "poids", "weight", "weightKg"))
                    .largeur(number(node, "largeur", "width"))
                    .longueur(number(node, "longueur", "length"))
                    .hauteur(number(node, "hauteur", "height"))
                    .poidsVolumetrique(number(node, "poidsVolumetrique", "volume", "volumetricWeight"))
                    .build()));
            return parcels;
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse conveyor response", ex);
        }
    }

    private void applyMeasurements(DocketRecord record, ConveyorParcelDto parcel) {
        if (parcel.getPoids() != null) record.setPoids(parcel.getPoids());
        if (parcel.getLargeur() != null) record.setLargeur(parcel.getLargeur());
        if (parcel.getLongueur() != null) record.setLongueur(parcel.getLongueur());
        if (parcel.getHauteur() != null) record.setHauteur(parcel.getHauteur());

        Double poidsVolumetrique = parcel.getPoidsVolumetrique();
        if (poidsVolumetrique == null && record.getLargeur() != null && record.getLongueur() != null && record.getHauteur() != null) {
            poidsVolumetrique = (record.getLargeur() * record.getLongueur() * record.getHauteur()) / 5000;
        }
        if (poidsVolumetrique != null) {
            record.setPoidsVolumetrique(poidsVolumetrique);
        }
    }

    private void applyMissingMeasurements(DocketRecord record, ConveyorParcelDto parcel) {
        if (record.getPoids() == null && parcel.getPoids() != null) record.setPoids(parcel.getPoids());
        if (record.getLargeur() == null && parcel.getLargeur() != null) record.setLargeur(parcel.getLargeur());
        if (record.getLongueur() == null && parcel.getLongueur() != null) record.setLongueur(parcel.getLongueur());
        if (record.getHauteur() == null && parcel.getHauteur() != null) record.setHauteur(parcel.getHauteur());

        if (record.getPoidsVolumetrique() == null) {
            Double poidsVolumetrique = parcel.getPoidsVolumetrique();
            if (poidsVolumetrique == null && record.getLargeur() != null && record.getLongueur() != null && record.getHauteur() != null) {
                poidsVolumetrique = (record.getLargeur() * record.getLongueur() * record.getHauteur()) / 5000;
            }
            if (poidsVolumetrique != null) {
                record.setPoidsVolumetrique(poidsVolumetrique);
            }
        }
    }

    private boolean isComplete(DocketRecord record) {
        return record.getPoids() != null
                && record.getLargeur() != null
                && record.getLongueur() != null
                && record.getHauteur() != null
                && record.getPoidsVolumetrique() != null;
    }

    private ReceptionRecoveryResponseDto mapToRecoveryResponse(List<DocketRecord> records) {
        DocketRecord first = records.get(0);
        long completedCount = records.stream().filter(this::isComplete).count();
        long totalRecords = records.size();

        return ReceptionRecoveryResponseDto.builder()
                .recoveryId(first.getReceptionRecoveryId())
                .receptionRecoveryId(first.getReceptionRecoveryId())
                .recoveryName(first.getRecoveryName())
                .recoveredAt(first.getRecoveredAt())
                .totalRecords(totalRecords)
                .completed(completedCount)
                .incomplete(totalRecords - completedCount)
                .completedCount(completedCount)
                .incompleteCount(totalRecords - completedCount)
                .build();
    }

    private ConveyorParcelDto mapToConveyorParcel(DocketRecord record) {
        return ConveyorParcelDto.builder()
                .connote(record.getConnote())
                .numeroBatch(record.getNumeroBatch())
                .lta(record.getLta())
                .ligne(record.getLigne())
                .poids(record.getPoids())
                .largeur(record.getLargeur())
                .longueur(record.getLongueur())
                .hauteur(record.getHauteur())
                .poidsVolumetrique(record.getPoidsVolumetrique())
                .build();
    }

    private List<ConveyorParcelDto> mockConveyorParcels(List<DocketRecord> records) {
        return records.stream()
                .map(record -> {
                    double seed = Math.abs(record.getConnote() != null ? record.getConnote().hashCode() : record.getId().hashCode());
                    double largeur = record.getLargeur() != null ? record.getLargeur() : 20 + (seed % 30);
                    double longueur = record.getLongueur() != null ? record.getLongueur() : 25 + (seed % 35);
                    double hauteur = record.getHauteur() != null ? record.getHauteur() : 10 + (seed % 20);
                    Double poidsVolumetrique = record.getPoidsVolumetrique() != null
                            ? record.getPoidsVolumetrique()
                            : (largeur * longueur * hauteur) / 5000;

                    return ConveyorParcelDto.builder()
                            .connote(record.getConnote())
                            .poids(record.getPoids() != null ? record.getPoids() : 1 + (seed % 25))
                            .largeur(largeur)
                            .longueur(longueur)
                            .hauteur(hauteur)
                            .poidsVolumetrique(poidsVolumetrique)
                            .build();
                })
                .toList();
    }

    private Map<String, Integer> readHeaders(Sheet sheet, DataFormatter formatter, List<DocketRecordImportErrorDto> errors) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            errors.add(error(1, "header", "Ligne header obligatoire"));
            return Map.of();
        }

        Map<String, Integer> columns = new java.util.LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (!header.isBlank()) {
                columns.put(header, cell.getColumnIndex());
            }
        }

        for (String header : CORRECTION_HEADERS) {
            if (!columns.containsKey(normalizeHeader(header))) {
                errors.add(error(1, header, "Colonne obligatoire manquante : " + header));
            }
        }
        return columns;
    }

    private Double parseDouble(Row row, Map<String, Integer> columns, String header, DataFormatter formatter,
                               List<DocketRecordImportErrorDto> errors) {
        String value = readText(row, columns, header, formatter);
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

    private String readText(Row row, Map<String, Integer> columns, String header, DataFormatter formatter) {
        Integer index = columns.get(normalizeHeader(header));
        if (index == null || row == null) {
            return "";
        }
        return normalizeText(formatter.formatCellValue(row.getCell(index)));
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < CORRECTION_HEADERS.size(); index++) {
            if (!normalizeText(formatter.formatCellValue(row.getCell(index))).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasRowErrors(List<DocketRecordImportErrorDto> errors, int rowNumber) {
        return errors.stream().anyMatch(error -> error.getRowNumber() == rowNumber);
    }

    private void validateExcelFile(MultipartFile file) {
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

    private boolean isConfigured(String url) {
        return url != null && !url.isBlank();
    }

    private TypeFlux resolveTypeFlux(String value) {
        if (value == null || value.isBlank()) {
            return TypeFlux.RECEPTION;
        }
        try {
            return TypeFlux.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("typeFlux invalide. Valeurs autorisees : RECEPTION, DISPATCH, EXPEDITION");
        }
    }

    private TypeFlux resolveRequiredTypeFlux(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("typeFlux obligatoire. Valeurs autorisees : RECEPTION, DISPATCH, EXPEDITION");
        }
        return resolveTypeFlux(value);
    }

    private List<DocketRecord> resolveRecordsForConveyor(Set<String> importBatchIds) {
        Set<String> normalizedBatchIds = normalizeImportBatchIds(importBatchIds);
        List<DocketRecord> records = docketRecordRepository.findByImportBatchIdIn(List.copyOf(normalizedBatchIds));
        Set<String> foundBatchIds = records.stream()
                .map(DocketRecord::getImportBatchId)
                .filter(batchId -> batchId != null && !batchId.isBlank())
                .collect(Collectors.toSet());
        Set<String> missingBatchIds = new LinkedHashSet<>(normalizedBatchIds);
        missingBatchIds.removeAll(foundBatchIds);
        if (!missingBatchIds.isEmpty()) {
            throw badRequest("Manifest batch introuvable ou vide : " + missingBatchIds);
        }
        return records;
    }

    private List<DocketRecord> resolveRecordsForConveyorResponse(TypeFlux typeFlux, Set<String> importBatchIds) {
        List<DocketRecord> records;
        if (importBatchIds != null && !importBatchIds.isEmpty()) {
            records = resolveRecordsForConveyor(importBatchIds);
        } else {
            return docketRecordRepository.findByTypeFluxAndConveyorSentTrueAndRecoveredAtIsNull(typeFlux);
        }

        return records.stream()
                .filter(record -> record.getTypeFlux() == typeFlux)
                .filter(record -> Boolean.TRUE.equals(record.getConveyorSent()))
                .filter(record -> record.getRecoveredAt() == null)
                .toList();
    }

    private Set<String> normalizeImportBatchIds(Set<String> importBatchIds) {
        if (importBatchIds == null || importBatchIds.isEmpty()) {
            throw badRequest("Selection des manifestes obligatoire");
        }
        Set<String> normalized = importBatchIds.stream()
                .filter(batchId -> batchId != null && !batchId.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw badRequest("Selection des manifestes obligatoire");
        }
        return normalized;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private DocketRecordUpdateResultDto updateResult(int totalRows, int updatedCount, List<DocketRecordImportErrorDto> errors) {
        return DocketRecordUpdateResultDto.builder()
                .totalRows(totalRows)
                .updatedCount(updatedCount)
                .skippedCount(totalRows - updatedCount)
                .errors(errors)
                .build();
    }

    private DocketRecordImportErrorDto error(int rowNumber, String field, String message) {
        return DocketRecordImportErrorDto.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }

    private JsonNode firstExisting(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name)) {
                return node.get(name);
            }
        }
        return null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private Double number(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull() || value.asText().isBlank()) {
                continue;
            }
            if (value.isNumber()) {
                return value.asDouble();
            }
            try {
                return Double.parseDouble(value.asText().replace(',', '.'));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return normalizeText(value)
                .replace("Ã©", "e")
                .replace("Ã¨", "e")
                .replace("Ãª", "e")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void setNumericCell(Row row, int index, Double value) {
        if (value != null) {
            row.createCell(index).setCellValue(value);
        } else {
            row.createCell(index).setBlank();
        }
    }
}
