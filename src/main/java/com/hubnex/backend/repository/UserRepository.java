package com.hubnex.backend.repository;

import com.hubnex.backend.model.User;
import com.hubnex.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByHub_Id(Long hubId);

    List<User> findByAgence_Id(Long agenceId);

    List<User> findByRole(Role role);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
