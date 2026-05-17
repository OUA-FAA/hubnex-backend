package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.UserRequestDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.model.Role;
import com.hubnex.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponseDto> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserResponseDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/by-hub/{hubId}")
    public List<UserResponseDto> getByHubId(@PathVariable Long hubId) {
        return userService.getByHubId(hubId);
    }

    @GetMapping("/by-agence/{agenceId}")
    public List<UserResponseDto> getUsersByAgence(@PathVariable Long agenceId) {
        return userService.getByAgenceId(agenceId);
    }

    @GetMapping("/by-role/{role}")
    public List<UserResponseDto> getByRole(@PathVariable Role role) {
        return userService.getByRole(role);
    }

    @PostMapping
    public UserResponseDto create(@Valid @RequestBody UserRequestDto dto) {
        return userService.create(dto);
    }

    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return userService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public UserResponseDto patch(@PathVariable Long id, @RequestBody UserRequestDto dto) {
        return userService.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public UserResponseDto delete(@PathVariable Long id) {
        return userService.delete(id);
    }
}
