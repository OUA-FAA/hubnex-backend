package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyResponseDto {
    private Long id;
    private String nom;
    private String ville;
    private String adresse;
    private String telephone;
    private String responsable;
    private Boolean active;
    private Long hubId;
    private String hubNom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
