package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.HubRequestDto;
import com.hubnex.backend.dto.response.HubResponseDto;
import com.hubnex.backend.dto.response.HubStatsResponseDto;
import com.hubnex.backend.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @GetMapping
    public List<HubResponseDto> getAll(@RequestParam(required = false) Boolean actif) {
        return hubService.getAll(actif);
    }

    @GetMapping("/{id}")
    public HubResponseDto getById(@PathVariable Long id) {
        return hubService.getById(id);
    }

    @GetMapping("/{id}/stats")
    public HubStatsResponseDto getStats(@PathVariable Long id) {
        return hubService.getStats(id);
    }

    @PostMapping
    public HubResponseDto create(@Valid @RequestBody HubRequestDto dto) {
        return hubService.create(dto);
    }

    @PutMapping("/{id}")
    public HubResponseDto update(@PathVariable Long id, @Valid @RequestBody HubRequestDto dto) {
        return hubService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public HubResponseDto patch(@PathVariable Long id, @RequestBody HubRequestDto dto) {
        return hubService.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public HubResponseDto delete(@PathVariable Long id) {
        return hubService.delete(id);
    }
}
