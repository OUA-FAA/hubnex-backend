package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.AgencyRequestDto;
import com.hubnex.backend.dto.response.AgencyResponseDto;
import com.hubnex.backend.dto.response.AgencyStatsResponseDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final HubRepository hubRepository;

    public List<AgencyResponseDto> getAll() {
        return agencyRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AgencyResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public AgencyStatsResponseDto getStats(Long id) {
        Agency agency = getEntityById(id);

        return AgencyStatsResponseDto.builder()
                .agenceId(agency.getId())
                .agenceNom(agency.getNom())
                .colisEnAttenteLivraison(0L)
                .build();
    }

    public List<AgencyResponseDto> getByHubId(Long hubId) {
        if (!hubRepository.existsById(hubId)) {
            throw new RuntimeException("Hub not found");
        }
        return agencyRepository.findByHub_Id(hubId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AgencyResponseDto> getByHubIds(List<Long> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return List.of();
        }
        return agencyRepository.findByHub_IdIn(hubIds).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AgencyResponseDto create(AgencyRequestDto dto) {
        Agency agency = Agency.builder()
                .nom(dto.getNom())
                .adresse(dto.getAdresse())
                .telephone(dto.getTelephone())
                .responsable(dto.getResponsable())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .hub(resolveHub(dto.getHubId()))
                .build();

        return mapToResponse(agencyRepository.save(agency));
    }

    public AgencyResponseDto update(Long id, AgencyRequestDto dto) {
        Agency agency = getEntityById(id);

        agency.setNom(dto.getNom());
        agency.setAdresse(dto.getAdresse());
        agency.setTelephone(dto.getTelephone());
        agency.setResponsable(dto.getResponsable());
        agency.setActive(dto.getActive() != null ? dto.getActive() : agency.getActive());
        agency.setHub(resolveHub(dto.getHubId()));

        return mapToResponse(agencyRepository.save(agency));
    }

    public AgencyResponseDto patch(Long id, AgencyRequestDto dto) {
        Agency agency = getEntityById(id);

        if (dto.getNom() != null) {
            agency.setNom(dto.getNom());
        }
        if (dto.getAdresse() != null) {
            agency.setAdresse(dto.getAdresse());
        }
        if (dto.getTelephone() != null) {
            agency.setTelephone(dto.getTelephone());
        }
        if (dto.getResponsable() != null) {
            agency.setResponsable(dto.getResponsable());
        }
        if (dto.getActive() != null) {
            agency.setActive(dto.getActive());
        }
        if (dto.getHubId() != null) {
            agency.setHub(resolveHub(dto.getHubId()));
        }

        return mapToResponse(agencyRepository.save(agency));
    }

    public AgencyResponseDto delete(Long id) {
        Agency agency = getEntityById(id);
        agency.setActive(false);
        return mapToResponse(agencyRepository.save(agency));
    }

    private Agency getEntityById(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agency not found"));
    }

    private Hub resolveHub(Long hubId) {
        if (hubId == null) {
            throw new RuntimeException("Hub is required for Agency");
        }
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    private AgencyResponseDto mapToResponse(Agency agency) {
        Hub hub = agency.getHub();

        return AgencyResponseDto.builder()
                .id(agency.getId())
                .nom(agency.getNom())
                .adresse(agency.getAdresse())
                .telephone(agency.getTelephone())
                .responsable(agency.getResponsable())
                .active(agency.getActive())
                .hubId(hub != null ? hub.getId() : null)
                .hubNom(hub != null ? hub.getNom() : null)
                .createdAt(agency.getCreatedAt())
                .updatedAt(agency.getUpdatedAt())
                .build();
    }
}
