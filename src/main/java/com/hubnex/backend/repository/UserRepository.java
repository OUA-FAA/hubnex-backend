package com.hubnex.backend.repository;

import com.hubnex.backend.model.User;
import com.hubnex.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByHub_Id(Long hubId);

    List<User> findByAgence_Id(Long agenceId);

    List<User> findByHubs_Id(Long hubId);

    List<User> findByAgences_Id(Long agenceId);

    List<User> findByRole(Role role);

    List<User> findByRoleEntity_Name(String roleName);

    long countByRole(Role role);

    long countByActifTrue();

    long countByActifFalse();

    Optional<User> findByLogin(String login);

    @EntityGraph(attributePaths = {
            "roleEntity",
            "roleEntity.groups",
            "roleEntity.groups.permissionActions",
            "roleEntity.groups.permissionModules",
            "roleEntity.permissions"
    })
    @Query("select user from User user where user.login = :login")
    Optional<User> findWithAccessByLogin(@Param("login") String login);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {
            "roleEntity",
            "roleEntity.groups",
            "roleEntity.groups.permissionActions",
            "roleEntity.groups.permissionModules",
            "roleEntity.permissions"
    })
    @Query("select user from User user where user.email = :email")
    Optional<User> findWithAccessByEmail(@Param("email") String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
