package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.EtiquetteRequestDto;
import com.hubnex.backend.dto.response.EtiquetteResponseDto;
import com.hubnex.backend.service.EtiquetteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etiquettes")
@RequiredArgsConstructor
public class EtiquetteController {

    private final EtiquetteService etiquetteService;

    @GetMapping
    public List<EtiquetteResponseDto> getAll() {
        return etiquetteService.getAll();
    }

    @GetMapping("/{id}")
    public EtiquetteResponseDto getById(@PathVariable Long id) {
        return etiquetteService.getById(id);
    }

    @Operation(summary = "Create etiquette")
    @PostMapping
    public EtiquetteResponseDto create(@Valid @RequestBody EtiquetteRequestDto dto) {
        return etiquetteService.create(dto);
    }

    @PutMapping("/{id}")
    public EtiquetteResponseDto update(@PathVariable Long id, @Valid @RequestBody EtiquetteRequestDto dto) {
        return etiquetteService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public EtiquetteResponseDto patch(@PathVariable Long id, @RequestBody EtiquetteRequestDto dto) {
        return etiquetteService.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        etiquetteService.delete(id);
    }
}
