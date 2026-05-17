package com.hubnex.backend.repository;

import com.hubnex.backend.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByAgencyId(Long agencyId);
}