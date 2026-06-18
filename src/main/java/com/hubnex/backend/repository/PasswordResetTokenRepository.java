package com.hubnex.backend.repository;

import com.hubnex.backend.model.PasswordResetToken;
import com.hubnex.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
