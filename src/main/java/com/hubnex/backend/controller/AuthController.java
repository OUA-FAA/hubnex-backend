package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.ChangePasswordRequestDto;
import com.hubnex.backend.dto.request.LoginRequestDto;
import com.hubnex.backend.dto.response.AuthResponseDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody LoginRequestDto dto) {
        return authService.login(dto);
    }

    @GetMapping("/me")
    public UserResponseDto me(Principal principal) {
        return authService.me(principal.getName());
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
    }

    @PutMapping("/password")
    public UserResponseDto changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequestDto dto) {
        return authService.changePassword(principal.getName(), dto);
    }
}
