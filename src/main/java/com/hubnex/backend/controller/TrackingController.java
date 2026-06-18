package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.TrackingRequestDto;
import com.hubnex.backend.dto.response.TrackingResponseDto;
import com.hubnex.backend.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping
    public List<TrackingResponseDto> getAll() {
        return trackingService.getAll();
    }

    @GetMapping("/{id}")
    public TrackingResponseDto getById(@PathVariable Long id) {
        return trackingService.getById(id);
    }

    @Operation(summary = "Add tracking")
    @PostMapping
    public TrackingResponseDto create(@Valid @RequestBody TrackingRequestDto dto) {
        return trackingService.create(dto);
    }
}
