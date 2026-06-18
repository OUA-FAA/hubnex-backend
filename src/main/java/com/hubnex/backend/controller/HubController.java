package com.hubnex.backend.controller;

import com.hubnex.backend.dto.imports.HubImportResultDto;
import com.hubnex.backend.dto.request.HubRequestDto;
import com.hubnex.backend.dto.response.HubResponseDto;
import com.hubnex.backend.dto.response.HubStatsResponseDto;
import com.hubnex.backend.service.HubImportService;
import com.hubnex.backend.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;
    private final HubImportService hubImportService;

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

    @Operation(summary = "Import hubs from Excel")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HubImportResultDto importExcel(@RequestParam("file") MultipartFile file) {
        return hubImportService.importHubsFromExcel(file);
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
