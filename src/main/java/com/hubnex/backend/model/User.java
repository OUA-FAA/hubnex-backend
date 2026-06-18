package com.hubnex.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String nomComplet;

    @Column(unique = true)
    private String email;

    private String telephone;

    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private RoleEntity roleEntity;

    @Column(nullable = false)
    private Boolean actif;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agency agence;

    @ManyToOne
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @JsonIgnore
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_hubs",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "hub_id")
    )
    private Set<Hub> hubs = new HashSet<>();

    @JsonIgnore
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_agences",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "agence_id")
    )
    private Set<Agency> agences = new HashSet<>();

    @JsonIgnore
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_cities",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "city_id")
    )
    private Set<City> cities = new HashSet<>();

    @Column(unique = true)
    private String token;

    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur")
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
        if (actif == null) {
            actif = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
