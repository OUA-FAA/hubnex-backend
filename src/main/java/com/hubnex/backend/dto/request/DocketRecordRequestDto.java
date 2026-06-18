package com.hubnex.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubnex.backend.model.EtatColis;
import com.hubnex.backend.model.TypeFlux;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordRequestDto {
    @NotBlank
    private String connote;
    private String nomDestinataire;
    private String telephoneDestinataire;
    private String adresseDestinataire;
    private String villeDestinataire;
    private String secteur;
    private EtatColis etat;
    private Double poids;
    private Double volume;
    private Double largeur;
    private Double longueur;
    private Double hauteur;
    private Double poidsVolumetrique;
    private String description;
    private String lta;
    private String numeroSac;
    private String numeroBatch;
    private String ligne;
    private String labelUrl;
    private String dateReception;
    private TypeFlux typeFlux;
    @JsonIgnore
    @Schema(hidden = true)
    private boolean dateReceptionPresent;
    private Long hubPrincipalId;
    private Long hubSecondaireId;
    private Long etiquetteId;

    public void setDateReception(String dateReception) {
        this.dateReception = dateReception;
        this.dateReceptionPresent = true;
    }
}
