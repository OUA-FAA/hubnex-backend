package com.hubnex.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hub")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nom;

    @Column(unique = true)
    private String barcode;

    private String adresse;
    private String telephone;

    @Column(nullable = false)
    private Boolean actif;

    @JsonIgnore
    @OneToMany(mappedBy = "hub")
    private List<Agency> agences;

    @JsonIgnore
    @OneToMany(mappedBy = "hub")
    private List<User> utilisateurs;

    @JsonIgnore
    @OneToMany(mappedBy = "hub")
    private List<Etiquette> etiquettes;

    @JsonIgnore
    @OneToMany(mappedBy = "hubPrincipal")
    private List<DocketRecord> docketRecordsPrincipaux;

    @JsonIgnore
    @OneToMany(mappedBy = "hubSecondaire")
    private List<DocketRecord> docketRecordsSecondaires;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (actif == null) {
            actif = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
