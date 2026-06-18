package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.EtiquetteRequestDto;
import com.hubnex.backend.dto.response.EtiquetteResponseDto;
import com.hubnex.backend.model.Etiquette;
import com.hubnex.backend.model.EtatEtiquette;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.repository.EtiquetteRepository;
import com.hubnex.backend.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtiquetteService {

    private final EtiquetteRepository etiquetteRepository;
    private final HubRepository hubRepository;

    @Transactional(readOnly = true)
    public List<EtiquetteResponseDto> getAll() {
        return etiquetteRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public EtiquetteResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    @Transactional
    public EtiquetteResponseDto create(EtiquetteRequestDto dto) {
        String reference = normalizeReference(dto.getReference());
        if (etiquetteRepository.existsByReference(reference)) {
            throw new RuntimeException("Etiquette reference already exists");
        }

        Etiquette etiquette = Etiquette.builder()
                .reference(reference)
                .etat(dto.getEtat() != null ? dto.getEtat() : EtatEtiquette.DISPONIBLE)
                .nombreCodes(dto.getNombreCodes())
                .parentBarcode(dto.getParentBarcode())
                .hub(getHub(dto.getHubId()))
                .build();

        return mapToResponse(etiquetteRepository.save(etiquette));
    }

    @Transactional
    public EtiquetteResponseDto update(Long id, EtiquetteRequestDto dto) {
        Etiquette etiquette = getEntityById(id);
        String reference = normalizeReference(dto.getReference());
        validateReferenceForUpdate(id, reference);

        etiquette.setReference(reference);
        etiquette.setEtat(dto.getEtat() != null ? dto.getEtat() : etiquette.getEtat());
        etiquette.setNombreCodes(dto.getNombreCodes());
        etiquette.setParentBarcode(dto.getParentBarcode());
        etiquette.setHub(getHub(dto.getHubId()));

        return mapToResponse(etiquetteRepository.save(etiquette));
    }

    @Transactional
    public EtiquetteResponseDto patch(Long id, EtiquetteRequestDto dto) {
        Etiquette etiquette = getEntityById(id);
        if (dto.getReference() != null) {
            String reference = normalizeReference(dto.getReference());
            validateReferenceForUpdate(id, reference);
            etiquette.setReference(reference);
        }
        if (dto.getEtat() != null) {
            etiquette.setEtat(dto.getEtat());
        }
        if (dto.getNombreCodes() != null) {
            if (dto.getNombreCodes() < 0) {
                throw new RuntimeException("nombreCodes must be greater than or equal to 0");
            }
            etiquette.setNombreCodes(dto.getNombreCodes());
        }
        if (dto.getParentBarcode() != null) {
            etiquette.setParentBarcode(dto.getParentBarcode());
        }
        if (dto.getHubId() != null) {
            etiquette.setHub(getHub(dto.getHubId()));
        }
        return mapToResponse(etiquetteRepository.save(etiquette));
    }

    @Transactional
    public void delete(Long id) {
        Etiquette etiquette = getEntityById(id);
        etiquette.setEtat(EtatEtiquette.ANNULE);
        etiquetteRepository.save(etiquette);
    }

    Etiquette getEntityById(Long id) {
        return etiquetteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etiquette not found"));
    }

    private Hub getHub(Long id) {
        return hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    private String normalizeReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return "ETQ-" + Year.now().getValue() + "-" + System.currentTimeMillis();
        }
        return reference.trim();
    }

    private void validateReferenceForUpdate(Long id, String reference) {
        etiquetteRepository.findByReference(reference)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Etiquette reference already exists");
                });
    }

    private EtiquetteResponseDto mapToResponse(Etiquette etiquette) {
        Hub hub = etiquette.getHub();
        return EtiquetteResponseDto.builder()
                .id(etiquette.getId())
                .reference(etiquette.getReference())
                .etat(etiquette.getEtat())
                .nombreCodes(etiquette.getNombreCodes())
                .parentBarcode(etiquette.getParentBarcode())
                .hubId(hub != null ? hub.getId() : null)
                .hubNom(hub != null ? hub.getNom() : null)
                .createdAt(etiquette.getCreatedAt())
                .updatedAt(etiquette.getUpdatedAt())
                .build();
    }
}
