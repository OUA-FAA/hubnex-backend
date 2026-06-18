package com.hubnex.backend.repository;

import com.hubnex.backend.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByAgence_Id(Long agenceId);

    List<City> findByAgence_IdIn(List<Long> agenceIds);

    List<City> findByAgence_IdAndActiveTrue(Long agenceId);

    List<City> findByAgence_Hub_Id(Long hubId);

    List<City> findByAgence_Hub_IdAndActiveTrue(Long hubId);

    List<City> findByAgenceIsNull();

    long countByActiveTrue();

    long countByActiveFalse();

    long countByAgence_Id(Long agenceId);

    boolean existsByCode(String code);

    Optional<City> findByCode(String code);
}
