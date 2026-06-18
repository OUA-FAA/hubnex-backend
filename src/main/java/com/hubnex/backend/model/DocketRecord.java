package com.hubnex.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "docket_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String connote;

    private String nomDestinataire;
    private String telephoneDestinataire;
    private String adresseDestinataire;
    private String villeDestinataire;
    private String secteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Column(name = "import_batch_id")
    private String importBatchId;

    private LocalDateTime importedAt;
    private String receptionRecoveryId;
    private String recoveryName;
    private LocalDateTime recoveredAt;
    private Boolean conveyorSent;
    private LocalDateTime conveyorSentAt;
    private String conveyorSendBatchId;

    @Enumerated(EnumType.STRING)
    private TypeFlux typeFlux;

    @Column(nullable = false)
    private Boolean estConvoyeur;

    @Column(nullable = false)
    private Boolean estDispatche;

    @Column(nullable = false)
    private Boolean estArrive;

    @Column(nullable = false)
    private Boolean apiCree;

    private LocalDateTime dateReception;
    private LocalDateTime dateDispatch;

    @ManyToOne
    @JoinColumn(name = "hub_principal_id")
    private Hub hubPrincipal;

    @ManyToOne
    @JoinColumn(name = "hub_secondaire_id")
    private Hub hubSecondaire;

    @ManyToOne
    @JoinColumn(name = "etiquette_id")
    private Etiquette etiquette;

    @JsonIgnore
    @OneToMany(mappedBy = "docketRecord")
    private List<Tracking> trackingEvenements;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        defaultValues();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        defaultValues();
    }

    private void defaultValues() {
        if (etat == null) {
            etat = EtatColis.CREE;
        }
        if (estConvoyeur == null) {
            estConvoyeur = false;
        }
        if (estDispatche == null) {
            estDispatche = false;
        }
        if (estArrive == null) {
            estArrive = false;
        }
        if (apiCree == null) {
            apiCree = false;
        }
        if (conveyorSent == null) {
            conveyorSent = false;
        }
    }
}
