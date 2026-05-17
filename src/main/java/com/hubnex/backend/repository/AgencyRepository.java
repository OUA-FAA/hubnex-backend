package com.hubnex.backend.repository;

import com.hubnex.backend.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    List<Agency> findByHub_Id(Long hubId);
}
