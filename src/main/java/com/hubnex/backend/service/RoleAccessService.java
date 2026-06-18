package com.hubnex.backend.service;

import com.hubnex.backend.dto.response.AccessRightResponseDto;
import com.hubnex.backend.dto.response.UserGroupAccessResponseDto;
import com.hubnex.backend.model.Group;
import com.hubnex.backend.model.Permission;
import com.hubnex.backend.model.RoleEntity;
import com.hubnex.backend.model.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RoleAccessService {

    private static final String ADMIN = "ADMIN";

    public Set<String> resolveAuthorities(User user) {
        Set<String> authorities = new LinkedHashSet<>();
        if (user.getRole() != null) {
            authorities.add("ROLE_" + user.getRole().name());
        }
        RoleEntity role = user.getRoleEntity();
        if (role != null && Boolean.TRUE.equals(role.getActive()) && hasText(role.getName())) {
            if (isAdminRoleName(role.getName())) {
                authorities.add("ROLE_ADMIN");
                authorities.add(ADMIN);
            } else {
                authorities.add("ROLE_" + normalize(role.getName()));
            }
            resolveFinalPermissions(role).forEach(permission -> addPermissionAuthorities(authorities, permission));
        }
        return authorities;
    }

    private void addPermissionAuthorities(Set<String> authorities, String permission) {
        authorities.add(permission);
        String[] parts = permission.split(":", 2);
        if (parts.length != 2) {
            return;
        }

        String module = parts[0];
        String action = parts[1];
        authorities.add(module + "_" + action);
        switch (module) {
            case "USERS" -> authorities.add("USER_" + action);
            case "HUBS" -> authorities.add("HUB_" + action);
            case "AGENCIES" -> authorities.add("AGENCE_" + action);
            case "CITIES" -> authorities.add("CITY_" + action);
            default -> {
                // The normalized plural authority is sufficient for other modules.
            }
        }
    }

    public List<AccessRightResponseDto> resolveAccessRights(User user) {
        RoleEntity role = user.getRoleEntity();
        if (role == null || !Boolean.TRUE.equals(role.getActive())) {
            return List.of();
        }

        Map<String, Set<String>> byModule = new LinkedHashMap<>();
        resolveFinalPermissions(role).forEach(permission -> {
            String[] parts = permission.split(":", 2);
            if (parts.length == 2) {
                byModule.computeIfAbsent(parts[0], ignored -> new LinkedHashSet<>()).add(parts[1]);
            }
        });
        return byModule.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> AccessRightResponseDto.builder()
                        .module(entry.getKey())
                        .actions(entry.getValue())
                        .build())
                .toList();
    }

    public List<UserGroupAccessResponseDto> resolveGroups(User user) {
        RoleEntity role = user.getRoleEntity();
        if (role == null || role.getGroups() == null) {
            return List.of();
        }
        return role.getGroups().stream()
                .sorted(Comparator.comparing(Group::getName))
                .map(group -> UserGroupAccessResponseDto.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .permissionActions(normalizeValues(group.getPermissionActions()))
                        .permissionModules(normalizeValues(group.getPermissionModules()))
                        .build())
                .toList();
    }

    public Set<String> resolveFinalPermissions(RoleEntity role) {
        Set<String> permissions = new LinkedHashSet<>();
        if (role.getPermissions() != null) {
            role.getPermissions().forEach(permission -> addDirectPermission(permission, permissions));
        }
        if (role.getGroups() != null) {
            role.getGroups().forEach(group -> {
                Set<String> actions = normalizeValues(group.getPermissionActions());
                Set<String> modules = normalizeValues(group.getPermissionModules());
                modules.forEach(module -> actions.forEach(action -> permissions.add(module + ":" + action)));
            });
        }
        return permissions;
    }

    private void addDirectPermission(Permission permission, Set<String> result) {
        String module = resolveModule(permission);
        String action = resolveAction(permission.getCode());
        if (module != null && action != null) {
            result.add(module + ":" + action);
        }
    }

    private String resolveAction(String code) {
        if (!hasText(code)) return null;
        String value = normalize(code);
        if (value.endsWith("_VIEW")) return "VIEW";
        if (value.endsWith("_CREATE") || value.endsWith("_IMPORT_EXCEL")) return "CREATE";
        if (value.endsWith("_DELETE")) return "DELETE";
        if (value.endsWith("_UPDATE") || value.endsWith("_ASSIGN_HUB")
                || value.endsWith("_ASSIGN_AGENCE") || value.endsWith("_UPLOAD_PHOTO")
                || value.endsWith("_MANAGE_USERS") || value.endsWith("_MANAGE_CITIES")
                || value.endsWith("_MANAGE_PERMISSIONS") || value.endsWith("_VALIDATE")) return "EDIT";
        return null;
    }

    private String resolveModule(Permission permission) {
        String code = hasText(permission.getCode()) ? normalize(permission.getCode()) : "";
        if (code.startsWith("USER_")) return "USERS";
        if (code.startsWith("GROUP_")) return "GROUPS";
        if (code.startsWith("COMPANY_")) return "COMPANIES";
        if (code.startsWith("CITY_")) return "CITIES";
        if (code.startsWith("AGENCE_")) return "AGENCIES";
        if (code.startsWith("HUB_")) return "HUBS";
        if (code.startsWith("DOCKET_RECORD_") || code.startsWith("LOGISTICS_MANIFESTE_")) return "MANIFESTE";
        if (code.startsWith("LOGISTICS_RECEPTION_") || code.startsWith("BON_RECEPTION_")) return "RECEPTION";
        if (code.startsWith("LOGISTICS_DISPATCH_")) return "DISPATCH";
        if (code.startsWith("EXPEDITION_")) return "EXPEDITION";
        if (code.startsWith("LOGISTICS_TRACKING_") || code.startsWith("TRACKING_")) return "TRACKING";
        if (code.startsWith("ROLE_")) return "ROLES_PERMISSIONS";
        if (hasText(permission.getModule())) return normalize(permission.getModule()).replace(' ', '_');
        return null;
    }

    private Set<String> normalizeValues(Set<String> values) {
        if (values == null) return Set.of();
        return values.stream().filter(this::hasText).map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isAdminRoleName(String roleName) {
        if (!hasText(roleName)) {
            return false;
        }
        String normalized = normalize(roleName).replace('-', '_');
        return ADMIN.equals(normalized)
                || "ROLE_ADMIN".equals(normalized)
                || "SUP_ADMIN".equals(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
