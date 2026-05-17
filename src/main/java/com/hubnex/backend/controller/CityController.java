package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.CityRequestDto;
import com.hubnex.backend.dto.response.CityResponseDto;
import com.hubnex.backend.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public List<CityResponseDto> getAll() {
        return cityService.getAll();
    }

    @GetMapping("/{id}")
    public CityResponseDto getById(@PathVariable Long id) {
        return cityService.getById(id);
    }

    @GetMapping("/by-agency/{agencyId}")
    public List<CityResponseDto> getByAgencyId(@PathVariable Long agencyId) {
        return cityService.getByAgencyId(agencyId);
    }

    @PostMapping
    public CityResponseDto create(@Valid @RequestBody CityRequestDto dto) {
        return cityService.create(dto);
    }

    @PutMapping("/{id}")
    public CityResponseDto update(@PathVariable Long id, @Valid @RequestBody CityRequestDto dto) {
        return cityService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cityService.delete(id);
    }
}
