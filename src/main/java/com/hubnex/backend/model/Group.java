package com.hubnex.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "group_permission_actions", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "action", nullable = false)
    private Set<String> permissionActions = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "group_permission_modules", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "module", nullable = false)
    private Set<String> permissionModules = new HashSet<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
