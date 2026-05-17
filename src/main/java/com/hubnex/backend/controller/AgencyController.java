package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.AgencyRequestDto;
import com.hubnex.backend.dto.response.AgencyResponseDto;
import com.hubnex.backend.dto.response.AgencyStatsResponseDto;
import com.hubnex.backend.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agences")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;

    @GetMapping
    public List<AgencyResponseDto> getAll() {
        return agencyService.getAll();
    }

    @GetMapping("/{id}")
    public AgencyResponseDto getById(@PathVariable Long id) {
        return agencyService.getById(id);
    }

    @GetMapping("/{id}/stats")
    public AgencyStatsResponseDto getStats(@PathVariable Long id) {
        return agencyService.getStats(id);
    }

    @GetMapping("/hub/{hubId}")
    public List<AgencyResponseDto> getByHubId(@PathVariable Long hubId) {
        return agencyService.getByHubId(hubId);
    }

    @PostMapping
    public AgencyResponseDto create(@Valid @RequestBody AgencyRequestDto dto) {
        return agencyService.create(dto);
    }

    @PutMapping("/{id}")
    public AgencyResponseDto update(@PathVariable Long id, @Valid @RequestBody AgencyRequestDto dto) {
        return agencyService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public AgencyResponseDto delete(@PathVariable Long id) {
        return agencyService.delete(id);
    }
}
