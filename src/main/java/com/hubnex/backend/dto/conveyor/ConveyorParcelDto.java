package com.hubnex.backend.dto.conveyor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConveyorParcelDto {
    private String connote;
    private String numeroBatch;
    private String lta;
    private String ligne;
    private Double poids;
    private Double largeur;
    private Double longueur;
    private Double hauteur;
    private Double poidsVolumetrique;
}
