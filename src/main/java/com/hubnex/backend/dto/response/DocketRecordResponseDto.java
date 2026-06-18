package com.hubnex.backend.dto.response;

import com.hubnex.backend.model.EtatColis;
import com.hubnex.backend.model.TypeFlux;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordResponseDto {
    private Long id;
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
    private String manifestName;
    private String importBatchId;
    private LocalDateTime importedAt;
    private String receptionRecoveryId;
    private String recoveryName;
    private LocalDateTime recoveredAt;
    private Boolean conveyorSent;
    private LocalDateTime conveyorSentAt;
    private String conveyorSendBatchId;
    private TypeFlux typeFlux;
    private Boolean estConvoyeur;
    private Boolean estDispatche;
    private Boolean estArrive;
    private Boolean apiCree;
    private LocalDateTime dateReception;
    private LocalDateTime dateDispatch;
    private Long hubPrincipalId;
    private String hubPrincipalNom;
    private Long hubSecondaireId;
    private String hubSecondaireNom;
    private Long etiquetteId;
    private String etiquetteReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
