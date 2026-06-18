package com.hubnex.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String adresse;
    private String telephone;
    private String responsable;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @JsonIgnore
    @OneToMany(mappedBy = "agence")
    private List<User> utilisateurs;

    @JsonIgnore
    @OneToMany(mappedBy = "agence")
    private List<City> cities;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
