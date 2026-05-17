package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.ChangePasswordRequestDto;
import com.hubnex.backend.dto.request.LoginRequestDto;
import com.hubnex.backend.dto.response.AuthResponseDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.UserRepository;
import com.hubnex.backend.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponseDto login(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getLogin(), dto.getMotDePasse()));

        User user = userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid login or password"));
        String token = jwtService.generateToken(user);

        return AuthResponseDto.builder()
                .token(token)
                .utilisateur(userService.getById(user.getId()))
                .build();
    }

    public UserResponseDto me(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userService.getById(user.getId());
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null) {
            tokenBlacklistService.blacklist(token);
        }
    }

    public UserResponseDto changePassword(String login, ChangePasswordRequestDto dto) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(dto.getAncienMotDePasse(), user.getMotDePasse())) {
            throw new BadCredentialsException("Old password is incorrect");
        }

        user.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        return userService.getById(userRepository.save(user).getId());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }
}
