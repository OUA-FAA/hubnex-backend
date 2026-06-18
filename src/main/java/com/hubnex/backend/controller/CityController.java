package com.hubnex.backend.controller;

import com.hubnex.backend.dto.imports.CityImportResultDto;
import com.hubnex.backend.dto.request.CityRequestDto;
import com.hubnex.backend.dto.response.CityMapDto;
import com.hubnex.backend.dto.response.CityResponseDto;
import com.hubnex.backend.service.CityImportService;
import com.hubnex.backend.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Slf4j
public class CityController {

    private final CityService cityService;
    private final CityImportService cityImportService;

    @GetMapping
    public List<CityResponseDto> getAll() {
        return cityService.getAll();
    }

    @GetMapping("/map")
    public List<CityMapDto> getMapCities() {
        return cityService.getMapCities();
    }

    @GetMapping("/{id}")
    public CityResponseDto getById(@PathVariable Long id) {
        return cityService.getById(id);
    }

    @GetMapping("/unassigned")
    public List<CityResponseDto> getUnassigned() {
        return cityService.getUnassigned();
    }

    @GetMapping("/by-agence/{agenceId}")
    public List<CityResponseDto> getByAgenceId(@PathVariable Long agenceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("cities by agence endpoint reached agenceId={} user={} authorities={}",
                agenceId,
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : List.of());
        return cityService.getByAgenceId(agenceId);
    }

    @GetMapping("/by-agences")
    public List<CityResponseDto> getByAgenceIds(@RequestParam(name = "agencyIds", required = false) List<Long> agencyIds,
                                                @RequestParam(name = "agenceIds", required = false) List<Long> agenceIds) {
        List<Long> ids = agencyIds != null ? agencyIds : agenceIds;
        return cityService.getByAgenceIds(ids);
    }

    @GetMapping("/by-agence/{agenceId}/active")
    public List<CityResponseDto> getActiveByAgenceId(@PathVariable Long agenceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("active cities by agence endpoint reached agenceId={} user={} authorities={}",
                agenceId,
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : List.of());
        return cityService.getActiveByAgenceId(agenceId);
    }

    @PostMapping
    public CityResponseDto create(@Valid @RequestBody CityRequestDto dto) {
        return cityService.create(dto);
    }

    @Operation(summary = "Import cities from Excel")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CityImportResultDto importExcel(@RequestParam("file") MultipartFile file) {
        return cityImportService.importCitiesFromExcel(file);
    }

    @PutMapping("/{id}")
    public CityResponseDto update(@PathVariable Long id, @Valid @RequestBody CityRequestDto dto) {
        return cityService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public CityResponseDto patch(@PathVariable Long id, @RequestBody CityRequestDto dto) {
        return cityService.patch(id, dto);
    }

    @PatchMapping("/{cityId}/assign-agence/{agenceId}")
    public CityResponseDto assignAgence(@PathVariable Long cityId, @PathVariable Long agenceId) {
        return cityService.assignAgence(cityId, agenceId);
    }

    @DeleteMapping("/{id}")
    public CityResponseDto delete(@PathVariable Long id) {
        return cityService.delete(id);
    }

    @DeleteMapping("/{cityId}/agence")
    public CityResponseDto removeAgence(@PathVariable Long cityId) {
        return cityService.removeAgence(cityId);
    }
}
