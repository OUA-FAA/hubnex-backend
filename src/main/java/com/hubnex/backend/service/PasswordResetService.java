package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.ForgotPasswordRequestDto;
import com.hubnex.backend.dto.request.ResetPasswordRequestDto;
import com.hubnex.backend.dto.response.MessageResponseDto;
import com.hubnex.backend.model.PasswordResetToken;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.PasswordResetTokenRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String FORGOT_PASSWORD_MESSAGE =
            "Si cet email existe, un lien de r\u00e9initialisation a \u00e9t\u00e9 envoy\u00e9.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${password.reset.expiration-minutes}")
    private long resetExpirationMinutes;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public MessageResponseDto forgotPassword(ForgotPasswordRequestDto dto) {
        userRepository.findByEmail(dto.getEmail())
                .filter(user -> Boolean.TRUE.equals(user.getActif()))
                .ifPresent(this::createTokenAndSendEmail);

        return MessageResponseDto.builder()
                .message(FORGOT_PASSWORD_MESSAGE)
                .build();
    }

    public MessageResponseDto resetPassword(ResetPasswordRequestDto dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de r\u00e9initialisation invalide"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de r\u00e9initialisation d\u00e9j\u00e0 utilis\u00e9");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de r\u00e9initialisation expir\u00e9");
        }

        User user = resetToken.getUser();
        user.setMotDePasse(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return MessageResponseDto.builder()
                .message("Mot de passe r\u00e9initialis\u00e9 avec succ\u00e8s.")
                .build();
    }

    private void createTokenAndSendEmail(User user) {
        invalidateExistingTokens(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpirationMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (RuntimeException ignored) {
            // Keep the public response generic even if local SMTP is not configured.
        }
    }

    private void invalidateExistingTokens(User user) {
        var activeTokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
        activeTokens.forEach(token -> token.setUsed(true));
        passwordResetTokenRepository.saveAll(activeTokens);
    }
}
