package com.hubnex.backend.repository;

import com.hubnex.backend.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HubRepository extends JpaRepository<Hub, Long> {
    List<Hub> findByActif(Boolean actif);

    long countByActifTrue();

    long countByActifFalse();

    boolean existsByNom(String nom);

    boolean existsByBarcode(String barcode);

    Optional<Hub> findByNom(String nom);

    Optional<Hub> findByNomIgnoreCase(String nom);

    Optional<Hub> findByBarcode(String barcode);
}
