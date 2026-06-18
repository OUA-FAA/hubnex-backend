package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.CityRequestDto;
import com.hubnex.backend.dto.response.CityMapDto;
import com.hubnex.backend.dto.response.CityResponseDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.City;
import com.hubnex.backend.model.Role;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.CityRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CityService {

    private static final Set<String> USERS_FORM_CITY_READ_AUTHORITIES = Set.of(
            "ROLE_ADMIN", "ADMIN",
            "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE",
            "AGENCIES:VIEW", "AGENCIES:CREATE", "AGENCIES_VIEW", "AGENCIES_CREATE",
            "AGENCE_VIEW", "AGENCE_CREATE",
            "CITIES:VIEW", "CITIES_VIEW", "CITY_VIEW", "CITY_CREATE"
    );

    private final CityRepository cityRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;

    public List<CityResponseDto> getAll() {
        return getCitiesForCurrentUser().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CityResponseDto getById(Long id) {
        City city = getEntityById(id);
        ensureCanReadCity(city);
        return mapToResponse(city);
    }

    public List<CityResponseDto> getByAgenceId(Long agenceId) {
        if (!agencyRepository.existsById(agenceId)) {
            throw new RuntimeException("Agence not found");
        }
        ensureCanReadAgence(agenceId);
        return cityRepository.findByAgence_Id(agenceId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CityResponseDto> getByAgenceIds(List<Long> agenceIds) {
        if (agenceIds == null || agenceIds.isEmpty()) {
            return List.of();
        }
        return cityRepository.findByAgence_IdIn(agenceIds).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CityResponseDto> getActiveByAgenceId(Long agenceId) {
        if (!agencyRepository.existsById(agenceId)) {
            throw new RuntimeException("Agence not found");
        }
        ensureCanReadAgence(agenceId);
        return cityRepository.findByAgence_IdAndActiveTrue(agenceId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CityResponseDto> getUnassigned() {
        return cityRepository.findByAgenceIsNull().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CityMapDto> getMapCities() {
        return getCitiesForCurrentUser().stream()
                .filter(city -> city.getLatitude() != null && city.getLongitude() != null)
                .map(this::mapToMapDto)
                .toList();
    }

    public CityResponseDto create(CityRequestDto dto) {
        validateName(dto.getName());
        validateCodeUniqueForCreate(dto.getCode());
        validateCoordinates(dto.getLatitude(), dto.getLongitude());

        City city = City.builder()
                .name(dto.getName())
                .code(normalizeCode(dto.getCode()))
                .active(dto.getActive() != null ? dto.getActive() : true)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .agence(resolveOptionalAgence(dto.getAgenceId()))
                .build();

        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto update(Long id, CityRequestDto dto) {
        City city = getEntityById(id);
        validateName(dto.getName());
        validateCodeUniqueForUpdate(id, dto.getCode());
        validateCoordinates(dto.getLatitude(), dto.getLongitude());

        city.setName(dto.getName());
        city.setCode(normalizeCode(dto.getCode()));
        city.setActive(dto.getActive() != null ? dto.getActive() : city.getActive());
        city.setLatitude(dto.getLatitude());
        city.setLongitude(dto.getLongitude());
        city.setAgence(resolveOptionalAgence(dto.getAgenceId()));

        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto patch(Long id, CityRequestDto dto) {
        City city = getEntityById(id);

        if (dto.getName() != null) {
            validateName(dto.getName());
            city.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            validateCodeUniqueForUpdate(id, dto.getCode());
            city.setCode(normalizeCode(dto.getCode()));
        }
        if (dto.getActive() != null) {
            city.setActive(dto.getActive());
        }
        if (dto.getLatitude() != null) {
            validateLatitude(dto.getLatitude());
            city.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            validateLongitude(dto.getLongitude());
            city.setLongitude(dto.getLongitude());
        }
        if (dto.getAgenceId() != null) {
            city.setAgence(resolveAgence(dto.getAgenceId()));
        }

        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto delete(Long id) {
        City city = getEntityById(id);
        city.setActive(false);
        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto assignAgence(Long cityId, Long agenceId) {
        City city = getEntityById(cityId);
        city.setAgence(resolveAgence(agenceId));
        return mapToResponse(cityRepository.save(city));
    }

    public CityResponseDto removeAgence(Long cityId) {
        City city = getEntityById(cityId);
        city.setAgence(null);
        return mapToResponse(cityRepository.save(city));
    }

    private City getEntityById(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("City not found"));
    }

    private Agency resolveAgence(Long agenceId) {
        if (agenceId == null) {
            throw new RuntimeException("agenceId is required");
        }
        return agencyRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence not found"));
    }

    private Agency resolveOptionalAgence(Long agenceId) {
        if (agenceId == null) {
            return null;
        }
        return resolveAgence(agenceId);
    }

    public List<City> getCitiesForCurrentUser() {
        if (hasUsersFormCityReadAuthority()) {
            return cityRepository.findAll();
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return cityRepository.findAll();
        }

        User user = currentUser.get();
        if (user.getRole() == Role.ADMIN) {
            return cityRepository.findAll();
        }

        return List.of();
    }

    private Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByLogin(authentication.getName());
    }

    private void ensureCanReadCity(City city) {
        Agency agence = city.getAgence();
        if (agence == null) {
            Optional<User> currentUser = getCurrentUser();
            if (hasUsersFormCityReadAuthority()
                    || currentUser.isEmpty()
                    || currentUser.get().getRole() == Role.ADMIN) {
                return;
            }
            throw new RuntimeException("Not allowed to read unassigned city");
        }
        ensureCanReadAgence(agence.getId());
    }

    private void ensureCanReadAgence(Long agenceId) {
        if (hasUsersFormCityReadAuthority()) {
            return;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return;
        }

        User user = currentUser.get();
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        throw new RuntimeException("Not allowed to read cities for this agence");
    }

    private boolean hasUsersFormCityReadAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(USERS_FORM_CITY_READ_AUTHORITIES::contains);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("City name is required");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    private void validateLatitude(Double latitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new RuntimeException("Latitude must be between -90 and 90");
        }
    }

    private void validateLongitude(Double longitude) {
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new RuntimeException("Longitude must be between -180 and 180");
        }
    }

    private void validateCodeUniqueForCreate(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode != null && cityRepository.existsByCode(normalizedCode)) {
            throw new RuntimeException("City code already exists");
        }
    }

    private void validateCodeUniqueForUpdate(Long cityId, String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return;
        }

        cityRepository.findByCode(normalizedCode)
                .filter(existing -> !existing.getId().equals(cityId))
                .ifPresent(existing -> {
                    throw new RuntimeException("City code already exists");
                });
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }

    private CityResponseDto mapToResponse(City city) {
        Agency agence = city.getAgence();

        return CityResponseDto.builder()
                .id(city.getId())
                .name(city.getName())
                .code(city.getCode())
                .active(city.getActive())
                .agenceId(agence != null ? agence.getId() : null)
                .agenceNom(agence != null ? agence.getNom() : null)
                .latitude(city.getLatitude())
                .longitude(city.getLongitude())
                .createdAt(city.getCreatedAt())
                .updatedAt(city.getUpdatedAt())
                .build();
    }

    private CityMapDto mapToMapDto(City city) {
        Agency agence = city.getAgence();

        return CityMapDto.builder()
                .id(city.getId())
                .name(city.getName())
                .code(city.getCode())
                .active(city.getActive())
                .agenceId(agence != null ? agence.getId() : null)
                .agenceNom(agence != null ? agence.getNom() : null)
                .latitude(city.getLatitude())
                .longitude(city.getLongitude())
                .agenceCount(agence != null ? 1 : 0)
                .build();
    }
}
