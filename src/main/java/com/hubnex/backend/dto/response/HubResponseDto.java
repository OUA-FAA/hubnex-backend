package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubResponseDto {
    private Long id;
    private String nom;
    private String barcode;
    private String adresse;
    private String telephone;
    private Boolean actif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
