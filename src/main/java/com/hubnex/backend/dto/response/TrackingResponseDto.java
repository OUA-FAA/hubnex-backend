package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingResponseDto {
    private Long id;
    private String statut;
    private String action;
    private String note;
    private String description;
    private LocalDateTime dateScan;
    private LocalDateTime dateAction;
    private Long utilisateurId;
    private String utilisateurNom;
    private Long docketRecordId;
    private String docketRecordConnote;
    private String connote;
    private Long hubId;
    private String hubNom;
    private String localisation;
    private Long colisId;
    private String colisConnote;
    private LocalDateTime createdAt;
}
