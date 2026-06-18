package com.hubnex.backend.dto.response;

import com.hubnex.backend.model.EtatEtiquette;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtiquetteResponseDto {
    private Long id;
    private String reference;
    private EtatEtiquette etat;
    private Integer nombreCodes;
    private String parentBarcode;
    private Long hubId;
    private String hubNom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
