package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.DocketRecordRequestDto;
import com.hubnex.backend.dto.response.DocketRecordManifestResponseDto;
import com.hubnex.backend.dto.response.DocketRecordResponseDto;
import com.hubnex.backend.model.*;
import com.hubnex.backend.repository.DocketRecordRepository;
import com.hubnex.backend.repository.EtiquetteRepository;
import com.hubnex.backend.repository.HubRepository;
import com.hubnex.backend.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocketRecordService {

    private final DocketRecordRepository docketRecordRepository;
    private final HubRepository hubRepository;
    private final EtiquetteRepository etiquetteRepository;
    private final TrackingRepository trackingRepository;

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getAll() {
        return docketRecordRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocketRecordResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public DocketRecordResponseDto getByConnote(String connote) {
        return mapToResponse(docketRecordRepository.findByConnote(connote)
                .orElseThrow(() -> new RuntimeException("Docket record not found")));
    }

    @Transactional(readOnly = true)
    public List<DocketRecordManifestResponseDto> getManifests() {
        return docketRecordRepository.findByImportBatchIdIsNotNull().stream()
                .collect(Collectors.groupingBy(DocketRecord::getImportBatchId, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(this::mapToManifestResponse)
                .sorted(Comparator.comparing(DocketRecordManifestResponseDto::getImportedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocketRecordResponseDto> getByImportBatchId(String importBatchId) {
        return docketRecordRepository.findByImportBatchId(importBatchId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteManifest(String importBatchId) {
        if (importBatchId == null || importBatchId.isBlank()) {
            throw new RuntimeException("Manifest import batch id is required");
        }

        List<DocketRecord> records = docketRecordRepository.findByImportBatchId(importBatchId);
        if (records.isEmpty()) {
            throw new RuntimeException("Manifest not found: " + importBatchId);
        }

        List<Long> recordIds = records.stream().map(DocketRecord::getId).toList();
        trackingRepository.deleteByDocketRecordIds(recordIds);
        docketRecordRepository.deleteAllInBatch(records);
    }

    @Transactional
    public DocketRecordResponseDto create(DocketRecordRequestDto dto) {
        if (docketRecordRepository.existsByConnote(dto.getConnote())) {
            throw new RuntimeException("Connote already exists");
        }

        DocketRecord record = new DocketRecord();
        applyFull(record, dto);
        if (record.getEtat() == null) {
            record.setEtat(EtatColis.CREE);
        }
        defaultBooleans(record);
        applyPoidsVolumetrique(record, dto.getPoidsVolumetrique());
        return mapToResponse(docketRecordRepository.save(record));
    }

    @Transactional
    public DocketRecordResponseDto update(Long id, DocketRecordRequestDto dto) {
        DocketRecord record = getEntityById(id);
        docketRecordRepository.findByConnote(dto.getConnote())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Connote already exists");
                });

        applyFull(record, dto);
        applyPoidsVolumetrique(record, dto.getPoidsVolumetrique());
        return mapToResponse(docketRecordRepository.save(record));
    }

    @Transactional
    public DocketRecordResponseDto patch(Long id, DocketRecordRequestDto dto) {
        DocketRecord record = getEntityById(id);
        if (dto.getConnote() != null) {
            docketRecordRepository.findByConnote(dto.getConnote())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new RuntimeException("Connote already exists");
                    });
            record.setConnote(dto.getConnote());
        }
        if (dto.getNomDestinataire() != null) record.setNomDestinataire(dto.getNomDestinataire());
        if (dto.getTelephoneDestinataire() != null) record.setTelephoneDestinataire(dto.getTelephoneDestinataire());
        if (dto.getAdresseDestinataire() != null) record.setAdresseDestinataire(dto.getAdresseDestinataire());
        if (dto.getVilleDestinataire() != null) record.setVilleDestinataire(dto.getVilleDestinataire());
        if (dto.getSecteur() != null) record.setSecteur(dto.getSecteur());
        if (dto.getEtat() != null) record.setEtat(dto.getEtat());
        if (dto.getPoids() != null) record.setPoids(dto.getPoids());
        if (dto.getVolume() != null) record.setVolume(dto.getVolume());
        if (dto.getLargeur() != null) record.setLargeur(dto.getLargeur());
        if (dto.getLongueur() != null) record.setLongueur(dto.getLongueur());
        if (dto.getHauteur() != null) record.setHauteur(dto.getHauteur());
        if (dto.getDescription() != null) record.setDescription(dto.getDescription());
        if (dto.getLta() != null) record.setLta(dto.getLta());
        if (dto.getNumeroSac() != null) record.setNumeroSac(dto.getNumeroSac());
        if (dto.getNumeroBatch() != null) record.setNumeroBatch(dto.getNumeroBatch());
        if (dto.getLigne() != null) record.setLigne(dto.getLigne());
        if (dto.getLabelUrl() != null) record.setLabelUrl(dto.getLabelUrl());
        if (dto.isDateReceptionPresent()) applyDateReception(record, dto.getDateReception());
        if (dto.getTypeFlux() != null) record.setTypeFlux(dto.getTypeFlux());
        if (dto.getHubPrincipalId() != null) record.setHubPrincipal(getHub(dto.getHubPrincipalId()));
        if (dto.getHubSecondaireId() != null) record.setHubSecondaire(getHub(dto.getHubSecondaireId()));
        if (dto.getEtiquetteId() != null) record.setEtiquette(getEtiquette(dto.getEtiquetteId()));
        applyPoidsVolumetrique(record, dto.getPoidsVolumetrique());
        return mapToResponse(docketRecordRepository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        DocketRecord record = getEntityById(id);
        record.setEtat(EtatColis.ANNULE);
        docketRecordRepository.save(record);
    }

    @Transactional
    public void markDispatchedByConnote(String connote) {
        docketRecordRepository.findByConnote(connote).ifPresent(record -> {
            record.setEstDispatche(true);
            record.setDateDispatch(java.time.LocalDateTime.now());
            record.setEtat(EtatColis.EN_TRANSIT);
            docketRecordRepository.save(record);
        });
    }

    @Transactional
    public void markReceivedByConnote(String connote, EtatColis etat) {
        docketRecordRepository.findByConnote(connote).ifPresent(record -> {
            record.setEstArrive(true);
            record.setDateReception(java.time.LocalDateTime.now());
            record.setEtat(etat != null ? etat : EtatColis.ARRIVE_HUB);
            docketRecordRepository.save(record);
        });
    }

    DocketRecord getEntityById(Long id) {
        return docketRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Docket record not found"));
    }

    private void applyFull(DocketRecord record, DocketRecordRequestDto dto) {
        record.setConnote(dto.getConnote());
        record.setNomDestinataire(dto.getNomDestinataire());
        record.setTelephoneDestinataire(dto.getTelephoneDestinataire());
        record.setAdresseDestinataire(dto.getAdresseDestinataire());
        record.setVilleDestinataire(dto.getVilleDestinataire());
        record.setSecteur(dto.getSecteur());
        record.setEtat(dto.getEtat() != null ? dto.getEtat() : record.getEtat());
        record.setPoids(dto.getPoids());
        record.setVolume(dto.getVolume());
        record.setLargeur(dto.getLargeur());
        record.setLongueur(dto.getLongueur());
        record.setHauteur(dto.getHauteur());
        record.setPoidsVolumetrique(dto.getPoidsVolumetrique());
        record.setDescription(dto.getDescription());
        record.setLta(dto.getLta());
        record.setNumeroSac(dto.getNumeroSac());
        record.setNumeroBatch(dto.getNumeroBatch());
        record.setLigne(dto.getLigne());
        record.setLabelUrl(dto.getLabelUrl());
        if (dto.isDateReceptionPresent()) {
            applyDateReception(record, dto.getDateReception());
        }
        if (dto.getTypeFlux() != null) {
            record.setTypeFlux(dto.getTypeFlux());
        }
        defaultBooleans(record);
        record.setHubPrincipal(dto.getHubPrincipalId() != null ? getHub(dto.getHubPrincipalId()) : null);
        record.setHubSecondaire(dto.getHubSecondaireId() != null ? getHub(dto.getHubSecondaireId()) : null);
        record.setEtiquette(dto.getEtiquetteId() != null ? getEtiquette(dto.getEtiquetteId()) : null);
    }

    private void defaultBooleans(DocketRecord record) {
        if (record.getEstConvoyeur() == null) record.setEstConvoyeur(false);
        if (record.getEstDispatche() == null) record.setEstDispatche(false);
        if (record.getEstArrive() == null) record.setEstArrive(false);
        if (record.getApiCree() == null) record.setApiCree(false);
        if (record.getConveyorSent() == null) record.setConveyorSent(false);
    }

    private void applyPoidsVolumetrique(DocketRecord record, Double explicitValue) {
        if (explicitValue != null) {
            record.setPoidsVolumetrique(explicitValue);
            return;
        }
        if (record.getLargeur() != null && record.getLongueur() != null && record.getHauteur() != null) {
            record.setPoidsVolumetrique((record.getLargeur() * record.getLongueur() * record.getHauteur()) / 5000);
        }
    }

    private void applyDateReception(DocketRecord record, String value) {
        LocalDateTime dateReception = parseDateReception(value);
        record.setDateReception(dateReception);
        if (dateReception == null) {
            record.setEstArrive(false);
            if (record.getEtat() == EtatColis.ARRIVE_HUB) {
                record.setEtat(EtatColis.CREE);
            }
            return;
        }

        record.setEtat(EtatColis.ARRIVE_HUB);
        record.setEstArrive(true);
    }

    private LocalDateTime parseDateReception(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // Try ISO datetime with offset, such as 2026-06-09T10:30:00Z.
        }

        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Try ISO date, such as 2026-06-09.
        }

        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("dateReception doit etre une date ISO valide, par exemple 2026-06-09 ou 2026-06-09T10:30:00");
        }
    }

    private Hub getHub(Long id) {
        return hubRepository.findById(id).orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    private Etiquette getEtiquette(Long id) {
        return etiquetteRepository.findById(id).orElseThrow(() -> new RuntimeException("Etiquette not found"));
    }

    public DocketRecordResponseDto mapToResponse(DocketRecord record) {
        Hub hubPrincipal = record.getHubPrincipal();
        Hub hubSecondaire = record.getHubSecondaire();
        Etiquette etiquette = record.getEtiquette();
        return DocketRecordResponseDto.builder()
                .id(record.getId())
                .connote(record.getConnote())
                .nomDestinataire(record.getNomDestinataire())
                .telephoneDestinataire(record.getTelephoneDestinataire())
                .adresseDestinataire(record.getAdresseDestinataire())
                .villeDestinataire(record.getVilleDestinataire())
                .secteur(record.getSecteur())
                .etat(record.getEtat())
                .poids(record.getPoids())
                .volume(record.getVolume())
                .largeur(record.getLargeur())
                .longueur(record.getLongueur())
                .hauteur(record.getHauteur())
                .poidsVolumetrique(record.getPoidsVolumetrique())
                .description(record.getDescription())
                .lta(record.getLta())
                .numeroSac(record.getNumeroSac())
                .numeroBatch(record.getNumeroBatch())
                .ligne(record.getLigne())
                .labelUrl(record.getLabelUrl())
                .manifestName(record.getManifestName())
                .importBatchId(record.getImportBatchId())
                .importedAt(record.getImportedAt())
                .receptionRecoveryId(record.getReceptionRecoveryId())
                .recoveryName(record.getRecoveryName())
                .recoveredAt(record.getRecoveredAt())
                .conveyorSent(record.getConveyorSent())
                .conveyorSentAt(record.getConveyorSentAt())
                .conveyorSendBatchId(record.getConveyorSendBatchId())
                .typeFlux(record.getTypeFlux())
                .estConvoyeur(record.getEstConvoyeur())
                .estDispatche(record.getEstDispatche())
                .estArrive(record.getEstArrive())
                .apiCree(record.getApiCree())
                .dateReception(record.getDateReception())
                .dateDispatch(record.getDateDispatch())
                .hubPrincipalId(hubPrincipal != null ? hubPrincipal.getId() : null)
                .hubPrincipalNom(hubPrincipal != null ? hubPrincipal.getNom() : null)
                .hubSecondaireId(hubSecondaire != null ? hubSecondaire.getId() : null)
                .hubSecondaireNom(hubSecondaire != null ? hubSecondaire.getNom() : null)
                .etiquetteId(etiquette != null ? etiquette.getId() : null)
                .etiquetteReference(etiquette != null ? etiquette.getReference() : null)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private DocketRecordManifestResponseDto mapToManifestResponse(List<DocketRecord> records) {
        DocketRecord first = records.get(0);
        Map<String, Long> statusSummary = records.stream()
                .collect(Collectors.groupingBy(record -> record.getEtat() != null ? record.getEtat().name() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.counting()));

        return DocketRecordManifestResponseDto.builder()
                .importBatchId(first.getImportBatchId())
                .manifestName(first.getManifestName())
                .importedAt(first.getImportedAt())
                .totalRecords((long) records.size())
                .statusSummary(statusSummary)
                .build();
    }
}
