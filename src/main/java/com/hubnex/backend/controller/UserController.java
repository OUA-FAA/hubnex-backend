package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.UserRequestDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public List<UserResponseDto> getByRole(@PathVariable String role) {
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

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponseDto uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return userService.uploadPhoto(id, file);
    }

    @DeleteMapping("/{id}/photo")
    public UserResponseDto deletePhoto(@PathVariable Long id) {
        return userService.deletePhoto(id);
    }

    @PostMapping("/{userId}/hubs/{hubId}")
    public UserResponseDto addHub(@PathVariable Long userId, @PathVariable Long hubId) {
        return userService.addHub(userId, hubId);
    }

    @DeleteMapping("/{userId}/hubs/{hubId}")
    public UserResponseDto removeHub(@PathVariable Long userId, @PathVariable Long hubId) {
        return userService.removeHub(userId, hubId);
    }

    @PostMapping("/{userId}/agences/{agenceId}")
    public UserResponseDto addAgence(@PathVariable Long userId, @PathVariable Long agenceId) {
        return userService.addAgence(userId, agenceId);
    }

    @DeleteMapping("/{userId}/agences/{agenceId}")
    public UserResponseDto removeAgence(@PathVariable Long userId, @PathVariable Long agenceId) {
        return userService.removeAgence(userId, agenceId);
    }

    @PostMapping("/{userId}/cities/{cityId}")
    public UserResponseDto addCity(@PathVariable Long userId, @PathVariable Long cityId) {
        return userService.addCity(userId, cityId);
    }

    @DeleteMapping("/{userId}/cities/{cityId}")
    public UserResponseDto removeCity(@PathVariable Long userId, @PathVariable Long cityId) {
        return userService.removeCity(userId, cityId);
    }

    @DeleteMapping("/{id}")
    public UserResponseDto delete(@PathVariable Long id) {
        return userService.delete(id);
    }
}
