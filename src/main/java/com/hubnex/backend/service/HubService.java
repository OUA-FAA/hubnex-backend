package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.HubRequestDto;
import com.hubnex.backend.dto.response.HubResponseDto;
import com.hubnex.backend.dto.response.HubStatsResponseDto;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;

    public List<HubResponseDto> getAll(Boolean actif) {
        List<Hub> hubs = actif == null ? hubRepository.findAll() : hubRepository.findByActif(actif);

        return hubs.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public HubResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public HubStatsResponseDto getStats(Long id) {
        Hub hub = getEntityById(id);

        return HubStatsResponseDto.builder()
                .hubId(hub.getId())
                .hubNom(hub.getNom())
                .colisTraites(0L)
                .bonsOuverts(0L)
                .build();
    }

    public HubResponseDto create(HubRequestDto dto) {
        validateUniqueForCreate(dto.getNom(), dto.getBarcode());

        Hub hub = Hub.builder()
                .nom(dto.getNom())
                .barcode(dto.getBarcode())
                .ville(dto.getVille())
                .adresse(dto.getAdresse())
                .telephone(dto.getTelephone())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();

        return mapToResponse(hubRepository.save(hub));
    }

    public HubResponseDto update(Long id, HubRequestDto dto) {
        Hub hub = getEntityById(id);
        validateUniqueForUpdate(id, dto.getNom(), dto.getBarcode());

        hub.setNom(dto.getNom());
        hub.setBarcode(dto.getBarcode());
        hub.setVille(dto.getVille());
        hub.setAdresse(dto.getAdresse());
        hub.setTelephone(dto.getTelephone());
        hub.setActif(dto.getActif() != null ? dto.getActif() : hub.getActif());

        return mapToResponse(hubRepository.save(hub));
    }

    public HubResponseDto patch(Long id, HubRequestDto dto) {
        Hub hub = getEntityById(id);

        if (dto.getNom() != null) {
            validateNomUniqueForUpdate(id, dto.getNom());
            hub.setNom(dto.getNom());
        }
        if (dto.getBarcode() != null) {
            validateBarcodeUniqueForUpdate(id, dto.getBarcode());
            hub.setBarcode(dto.getBarcode());
        }
        if (dto.getVille() != null) {
            hub.setVille(dto.getVille());
        }
        if (dto.getAdresse() != null) {
            hub.setAdresse(dto.getAdresse());
        }
        if (dto.getTelephone() != null) {
            hub.setTelephone(dto.getTelephone());
        }
        if (dto.getActif() != null) {
            hub.setActif(dto.getActif());
        }

        return mapToResponse(hubRepository.save(hub));
    }

    public HubResponseDto delete(Long id) {
        Hub hub = getEntityById(id);
        hub.setActif(false);
        return mapToResponse(hubRepository.save(hub));
    }

    private Hub getEntityById(Long id) {
        return hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    private void validateUniqueForCreate(String nom, String barcode) {
        if (hubRepository.existsByNom(nom)) {
            throw new RuntimeException("Hub name already exists");
        }
        if (barcode != null && hubRepository.existsByBarcode(barcode)) {
            throw new RuntimeException("Hub barcode already exists");
        }
    }

    private void validateUniqueForUpdate(Long hubId, String nom, String barcode) {
        validateNomUniqueForUpdate(hubId, nom);
        validateBarcodeUniqueForUpdate(hubId, barcode);
    }

    private void validateNomUniqueForUpdate(Long hubId, String nom) {
        hubRepository.findByNom(nom)
                .filter(existing -> !existing.getId().equals(hubId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Hub name already exists");
                });
    }

    private void validateBarcodeUniqueForUpdate(Long hubId, String barcode) {
        if (barcode == null) {
            return;
        }
        hubRepository.findByBarcode(barcode)
                .filter(existing -> !existing.getId().equals(hubId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Hub barcode already exists");
                });
    }

    private HubResponseDto mapToResponse(Hub hub) {
        return HubResponseDto.builder()
                .id(hub.getId())
                .nom(hub.getNom())
                .barcode(hub.getBarcode())
                .ville(hub.getVille())
                .adresse(hub.getAdresse())
                .telephone(hub.getTelephone())
                .actif(hub.getActif())
                .createdAt(hub.getCreatedAt())
                .updatedAt(hub.getUpdatedAt())
                .build();
    }
}
