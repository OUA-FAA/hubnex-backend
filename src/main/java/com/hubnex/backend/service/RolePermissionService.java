package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.RoleRequestDto;
import com.hubnex.backend.dto.response.PermissionResponseDto;
import com.hubnex.backend.dto.response.RoleGroupResponseDto;
import com.hubnex.backend.dto.response.RoleResponseDto;
import com.hubnex.backend.model.Group;
import com.hubnex.backend.model.Permission;
import com.hubnex.backend.model.RoleEntity;
import com.hubnex.backend.repository.GroupRepository;
import com.hubnex.backend.repository.PermissionRepository;
import com.hubnex.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final String ADMIN = "ADMIN";
    private static final String SUP_ADMIN_DISPLAY_NAME = "Sup-Admin";
    private static final String SYSTEM_ROLE_TYPE = "SYSTEM";
    private static final String DYNAMIC_ROLE_TYPE = "DYNAMIC";
    private static final Set<String> SYSTEM_ROLE_NAMES = Set.of(ADMIN);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(role -> !isDuplicateAdminAlias(role))
                .sorted(Comparator.comparing(RoleEntity::getName))
                .map(this::mapToRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponseDto getRoleById(Long id) {
        return mapToRoleResponse(getRoleEntityById(id));
    }

    @Transactional
    public RoleResponseDto createRole(RoleRequestDto dto) {
        String name = normalizeRequiredName(dto.getName());
        if (isAdminRoleName(name)) {
            throw badRequest("Sup-Admin is a system role and cannot be created as a dynamic role");
        }
        if (isOldFixedRoleName(name)) {
            throw badRequest("This old fixed role name cannot be recreated");
        }
        if (roleRepository.existsByName(name)) {
            throw badRequest("Role name already exists");
        }

        RoleEntity role = RoleEntity.builder()
                .name(name)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .systemRole(false)
                .groups(resolveGroups(dto.getGroupIds()))
                .build();

        return mapToRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponseDto updateRole(Long id, RoleRequestDto dto) {
        RoleEntity role = getRoleEntityById(id);
        if (isSystemRole(role)) {
            role.setActive(true);
            role.setSystemRole(true);
            role.setPermissions(new LinkedHashSet<>(permissionRepository.findAll()));
            throw badRequest("Sup-Admin is a system role and cannot be edited as a dynamic role");
        } else {
            String name = normalizeRequiredName(dto.getName());
            if (isAdminRoleName(name)) {
                throw badRequest("Sup-Admin is a system role and cannot be assigned as a dynamic role name");
            }
            validateNameUniqueForUpdate(id, name);
            role.setName(name);
            role.setActive(dto.getActive() != null ? dto.getActive() : true);
        }
        role.setGroups(resolveGroups(dto.getGroupIds()));

        return mapToRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponseDto patchRole(Long id, RoleRequestDto dto) {
        RoleEntity role = getRoleEntityById(id);

        if (isSystemRole(role)) {
            role.setActive(true);
            role.setSystemRole(true);
            role.setPermissions(new LinkedHashSet<>(permissionRepository.findAll()));
            throw badRequest("Sup-Admin is a system role and cannot be edited as a dynamic role");
        } else if (dto.getName() != null) {
            String name = normalizeRequiredName(dto.getName());
            if (isAdminRoleName(name)) {
                throw badRequest("Sup-Admin is a system role and cannot be assigned as a dynamic role name");
            }
            validateNameUniqueForUpdate(id, name);
            role.setName(name);
        }
        if (!isSystemRole(role) && dto.getActive() != null) {
            role.setActive(dto.getActive());
        }
        if (dto.getGroupIds() != null) {
            role.setGroups(resolveGroups(dto.getGroupIds()));
        }

        return mapToRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        RoleEntity role = getRoleEntityById(id);
        if (isSystemRole(role)) {
            throw badRequest("System roles cannot be deleted");
        }
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            throw badRequest("Role cannot be deleted while permissions are linked");
        }
        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                .map(this::mapToPermissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDto> getRolePermissions(Long roleId) {
        RoleEntity role = getRoleEntityById(roleId);
        return role.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                .map(this::mapToPermissionResponse)
                .toList();
    }

    @Transactional
    public RoleResponseDto updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        RoleEntity role = getRoleEntityById(roleId);
        if (isSystemRole(role)) {
            role.setPermissions(new LinkedHashSet<>(permissionRepository.findAll()));
            role.setActive(true);
            role.setSystemRole(true);
        } else {
            role.setPermissions(resolvePermissions(permissionIds));
        }
        return mapToRoleResponse(roleRepository.save(role));
    }

    private RoleEntity getRoleEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private Set<Group> resolveGroups(Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (groupIds.stream().anyMatch(id -> id == null)) {
            throw badRequest("groupIds must not contain null values");
        }

        List<Group> groups = groupRepository.findAllById(groupIds);
        Set<Long> foundIds = groups.stream()
                .map(Group::getId)
                .collect(Collectors.toSet());
        Set<Long> missingIds = new HashSet<>(groupIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw badRequest("Group(s) not found: " + missingIds);
        }

        groups.sort(Comparator.comparing(Group::getName));
        return new LinkedHashSet<>(groups);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (permissionIds.stream().anyMatch(id -> id == null)) {
            throw badRequest("permissionIds must not contain null values");
        }

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw badRequest("One or more permissions were not found");
        }
        permissions.sort(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode));
        return new LinkedHashSet<>(permissions);
    }

    private void validateNameUniqueForUpdate(Long roleId, String name) {
        roleRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(roleId))
                .ifPresent(existing -> {
                    throw badRequest("Role name already exists");
                });
    }

    private String normalizeRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw badRequest("Role name is required");
        }
        String normalized = name.trim();
        return normalized;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private RoleResponseDto mapToRoleResponse(RoleEntity role) {
        Set<Group> groups = role.getGroups() != null ? role.getGroups() : Set.of();
        Set<Permission> directPermissions = role.getPermissions() != null ? role.getPermissions() : Set.of();
        Set<String> permissionActions = new LinkedHashSet<>();
        Set<String> permissionModules = new LinkedHashSet<>();
        Set<String> finalPermissions = new LinkedHashSet<>();

        directPermissions.stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                .forEach(permission -> addPermission(permission, permissionActions, permissionModules, finalPermissions));

        groups.stream()
                .sorted(Comparator.comparing(Group::getName))
                .forEach(group -> addGroupPermissions(group, permissionActions, permissionModules, finalPermissions));

        return RoleResponseDto.builder()
                .id(role.getId())
                .name(displayRoleName(role.getName()))
                .technicalName(role.getName())
                .displayName(displayRoleName(role.getName()))
                .type(resolveRoleType(role))
                .roleType(resolveRoleType(role))
                .active(Boolean.TRUE.equals(role.getActive()))
                .systemRole(isSystemRole(role))
                .groups(groups.stream()
                        .sorted(Comparator.comparing(Group::getName))
                        .map(this::mapToRoleGroupResponse)
                        .toList())
                .permissions(directPermissions.stream()
                        .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                        .map(this::mapToPermissionResponse)
                        .toList())
                .permissionActions(permissionActions)
                .permissionModules(permissionModules)
                .finalPermissions(finalPermissions)
                .permissionCount(finalPermissions.size())
                .coveredModuleCount(permissionModules.size())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private boolean isSystemRole(RoleEntity role) {
        return role != null && isAdminRoleName(role.getName());
    }

    private String resolveRoleType(RoleEntity role) {
        return isSystemRole(role) ? SYSTEM_ROLE_TYPE : DYNAMIC_ROLE_TYPE;
    }

    private boolean isDuplicateAdminAlias(RoleEntity role) {
        return role != null
                && role.getName() != null
                && !ADMIN.equalsIgnoreCase(role.getName().trim())
                && isAdminRoleName(role.getName());
    }

    private boolean isOldFixedRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("AGENT_")
                || ("CON" + "VOYEUR").equals(normalized);
    }

    private boolean isAdminRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return SYSTEM_ROLE_NAMES.contains(normalized)
                || "ROLE_ADMIN".equals(normalized)
                || "SUP_ADMIN".equals(normalized);
    }

    private String displayRoleName(String roleName) {
        if (isAdminRoleName(roleName)) {
            return SUP_ADMIN_DISPLAY_NAME;
        }
        return roleName;
    }

    private void addPermission(Permission permission, Set<String> actions, Set<String> modules, Set<String> finalPermissions) {
        String action = resolveAction(permission.getCode());
        String module = resolveModule(permission);
        if (action == null || module == null) {
            return;
        }
        actions.add(action);
        modules.add(module);
        finalPermissions.add(module + ":" + action);
    }

    private void addGroupPermissions(Group group, Set<String> actions, Set<String> modules, Set<String> finalPermissions) {
        Set<String> groupActions = normalizeValues(group.getPermissionActions());
        Set<String> groupModules = normalizeValues(group.getPermissionModules());
        actions.addAll(groupActions);
        modules.addAll(groupModules);
        for (String module : groupModules) {
            for (String action : groupActions) {
                finalPermissions.add(module + ":" + action);
            }
        }
    }

    private Set<String> normalizeValues(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveAction(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.toUpperCase(Locale.ROOT);
        if (normalized.endsWith("_VIEW")) return "VIEW";
        if (normalized.endsWith("_CREATE") || normalized.endsWith("_IMPORT_EXCEL")) return "CREATE";
        if (normalized.endsWith("_DELETE")) return "DELETE";
        if (normalized.endsWith("_UPDATE")
                || normalized.endsWith("_ASSIGN_HUB")
                || normalized.endsWith("_ASSIGN_AGENCE")
                || normalized.endsWith("_ASSIGN_AGENCE")
                || normalized.endsWith("_UPLOAD_PHOTO")
                || normalized.endsWith("_MANAGE_USERS")
                || normalized.endsWith("_MANAGE_CITIES")
                || normalized.endsWith("_MANAGE_PERMISSIONS")
                || normalized.endsWith("_VALIDATE")) {
            return "EDIT";
        }
        return null;
    }

    private String resolveModule(Permission permission) {
        String code = permission.getCode() != null ? permission.getCode().toUpperCase(Locale.ROOT) : "";
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
        return null;
    }

    private RoleGroupResponseDto mapToRoleGroupResponse(Group group) {
        return RoleGroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .build();
    }

    private PermissionResponseDto mapToPermissionResponse(Permission permission) {
        return PermissionResponseDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .label(permission.getLabel())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }
}
