package com.hubnex.backend.config;

import com.hubnex.backend.model.Role;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial.login}")
    private String initialAdminLogin;

    @Value("${admin.initial.password}")
    private String initialAdminPassword;

    @Value("${admin.initial.email}")
    private String initialAdminEmail;

    @Value("${admin.initial.nom-complet}")
    private String initialAdminNomComplet;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .login(initialAdminLogin)
                .motDePasse(passwordEncoder.encode(initialAdminPassword))
                .nomComplet(initialAdminNomComplet)
                .email(initialAdminEmail)
                .role(Role.ADMIN)
                .actif(true)
                .build();

        userRepository.save(admin);
    }
}
