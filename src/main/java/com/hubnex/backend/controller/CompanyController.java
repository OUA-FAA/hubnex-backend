package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.CompanyRequestDto;
import com.hubnex.backend.dto.response.CompanyResponseDto;
import com.hubnex.backend.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public List<CompanyResponseDto> getAll() {
        return companyService.getAll();
    }

    @GetMapping("/{id}")
    public CompanyResponseDto getById(@PathVariable Long id) {
        return companyService.getById(id);
    }

    @PostMapping
    public CompanyResponseDto create(@Valid @RequestBody CompanyRequestDto dto) {
        return companyService.create(dto);
    }

    @PutMapping("/{id}")
    public CompanyResponseDto update(@PathVariable Long id, @Valid @RequestBody CompanyRequestDto dto) {
        return companyService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}
