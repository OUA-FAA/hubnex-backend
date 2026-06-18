package com.hubnex.backend.repository;

import com.hubnex.backend.model.Etiquette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtiquetteRepository extends JpaRepository<Etiquette, Long> {
    Optional<Etiquette> findByReference(String reference);

    boolean existsByReference(String reference);
}
