package com.hubnex.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("R\u00e9initialisation de votre mot de passe HuBnex");
        message.setText("""
                Bonjour,

                Cliquez sur ce lien pour r\u00e9initialiser votre mot de passe :
                %s

                Ce lien expire dans 30 minutes.
                """.formatted(resetLink));

        mailSender.send(message);
    }
}
