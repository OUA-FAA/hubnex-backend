package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.RoleRequestDto;
import com.hubnex.backend.dto.request.UpdateRolePermissionsDto;
import com.hubnex.backend.dto.response.PermissionResponseDto;
import com.hubnex.backend.dto.response.RoleResponseDto;
import com.hubnex.backend.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @Operation(summary = "List roles")
    @GetMapping("/api/roles")
    public List<RoleResponseDto> getAllRoles() {
        return rolePermissionService.getAllRoles();
    }

    @Operation(summary = "Get role by id")
    @GetMapping("/api/roles/{id}")
    public RoleResponseDto getRoleById(@PathVariable Long id) {
        return rolePermissionService.getRoleById(id);
    }

    @Operation(summary = "Create role")
    @PostMapping("/api/roles")
    public RoleResponseDto createRole(@Valid @RequestBody RoleRequestDto dto) {
        return rolePermissionService.createRole(dto);
    }

    @Operation(summary = "Update role")
    @PutMapping("/api/roles/{id}")
    public RoleResponseDto updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequestDto dto) {
        return rolePermissionService.updateRole(id, dto);
    }

    @Operation(summary = "Patch role")
    @PatchMapping("/api/roles/{id}")
    public RoleResponseDto patchRole(@PathVariable Long id, @RequestBody RoleRequestDto dto) {
        return rolePermissionService.patchRole(id, dto);
    }

    @Operation(summary = "Delete role")
    @DeleteMapping("/api/roles/{id}")
    public void deleteRole(@PathVariable Long id) {
        rolePermissionService.deleteRole(id);
    }

    @Operation(summary = "List permissions")
    @GetMapping("/api/permissions")
    public List<PermissionResponseDto> getAllPermissions() {
        return rolePermissionService.getAllPermissions();
    }

    @Operation(summary = "List role permissions")
    @GetMapping("/api/roles/{roleId}/permissions")
    public List<PermissionResponseDto> getRolePermissions(@PathVariable Long roleId) {
        return rolePermissionService.getRolePermissions(roleId);
    }

    @Operation(summary = "Update role permissions")
    @PutMapping("/api/roles/{roleId}/permissions")
    public RoleResponseDto updateRolePermissions(@PathVariable Long roleId,
                                                 @RequestBody UpdateRolePermissionsDto dto) {
        return rolePermissionService.updateRolePermissions(roleId, dto.getPermissionIds());
    }

    @Operation(summary = "Patch role permissions")
    @PatchMapping("/api/roles/{roleId}/permissions")
    public RoleResponseDto patchRolePermissions(@PathVariable Long roleId,
                                                @RequestBody UpdateRolePermissionsDto dto) {
        return rolePermissionService.updateRolePermissions(roleId, dto.getPermissionIds());
    }
}
