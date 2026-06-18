package com.hubnex.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "etiquette")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiquette {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatEtiquette etat;

    @Column(nullable = false)
    private Integer nombreCodes;

    private String parentBarcode;

    @ManyToOne
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @JsonIgnore
    @OneToMany(mappedBy = "etiquette")
    private List<DocketRecord> docketRecords;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (etat == null) {
            etat = EtatEtiquette.DISPONIBLE;
        }
        if (nombreCodes == null) {
            nombreCodes = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
