package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.ChangePasswordRequestDto;
import com.hubnex.backend.dto.request.LoginRequestDto;
import com.hubnex.backend.dto.response.AuthResponseDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.UserRepository;
import com.hubnex.backend.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INACTIVE_ACCOUNT_MESSAGE =
            "Votre compte est d\u00e9sactiv\u00e9. Contactez l\u2019administrateur.";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponseDto login(LoginRequestDto dto) {
        String email = dto.getEmail();
        User user = userRepository.findWithAccessByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"));

        if (!Boolean.TRUE.equals(user.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, INACTIVE_ACCOUNT_MESSAGE);
        }

        if (!passwordEncoder.matches(dto.getMotDePasse(), user.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide");
        }

        String token = jwtService.generateToken(user);

        UserResponseDto userResponse = userService.getAuthenticatedProfile(user.getId());
        return AuthResponseDto.builder()
                .token(token)
                .utilisateur(userResponse)
                .userId(userResponse.getId())
                .login(userResponse.getLogin())
                .email(userResponse.getEmail())
                .roleId(userResponse.getRoleId())
                .roleName(userResponse.getRoleName())
                .groups(userResponse.getGroups())
                .accessRights(userResponse.getAccessRights())
                .permissions(userResponse.getPermissions())
                .build();
    }

    public UserResponseDto me(String login) {
        User user = userRepository.findWithAccessByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userService.getAuthenticatedProfile(user.getId());
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
        return userService.getAuthenticatedProfile(userRepository.save(user).getId());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }
}
