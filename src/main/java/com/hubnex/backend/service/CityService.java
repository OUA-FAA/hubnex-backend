package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.CityRequestDto;
import com.hubnex.backend.dto.response.CityResponseDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.City;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final AgencyRepository agencyRepository;

    public List<CityResponseDto> getAll() {
        return cityRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CityResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public List<CityResponseDto> getByAgencyId(Long agencyId) {
        return cityRepository.findByAgencyId(agencyId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CityResponseDto create(CityRequestDto dto) {
        City city = City.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .agencyId(dto.getAgencyId())
                .build();

        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto update(Long id, CityRequestDto dto) {
        City city = getEntityById(id);

        city.setName(dto.getName());
        city.setCode(dto.getCode());
        city.setAgencyId(dto.getAgencyId());

        return mapToResponse(cityRepository.save(city));
    }

    public void delete(Long id) {
        cityRepository.deleteById(id);
    }

    private City getEntityById(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("City not found"));
    }

    private CityResponseDto mapToResponse(City city) {
        String agencyNom = null;
        if (city.getAgencyId() != null) {
            agencyNom = agencyRepository.findById(city.getAgencyId())
                    .map(Agency::getNom)
                    .orElse(null);
        }

        return CityResponseDto.builder()
                .id(city.getId())
                .name(city.getName())
                .code(city.getCode())
                .agencyId(city.getAgencyId())
                .agencyNom(agencyNom)
                .build();
    }
}
