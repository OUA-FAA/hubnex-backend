package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.CompanyRequestDto;
import com.hubnex.backend.dto.response.CompanyResponseDto;
import com.hubnex.backend.model.Company;
import com.hubnex.backend.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyResponseDto> getAll() {
        return companyRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CompanyResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public CompanyResponseDto create(CompanyRequestDto dto) {
        Company company = Company.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return mapToResponse(companyRepository.save(company));
    }

    public CompanyResponseDto update(Long id, CompanyRequestDto dto) {
        Company company = getEntityById(id);

        company.setName(dto.getName());
        company.setCode(dto.getCode());
        company.setEmail(dto.getEmail());
        company.setPhone(dto.getPhone());
        company.setAddress(dto.getAddress());
        company.setActive(dto.getActive() != null ? dto.getActive() : company.getActive());

        return mapToResponse(companyRepository.save(company));
    }

    public void delete(Long id) {
        companyRepository.deleteById(id);
    }

    private Company getEntityById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    private CompanyResponseDto mapToResponse(Company company) {
        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .active(company.getActive())
                .build();
    }
}
