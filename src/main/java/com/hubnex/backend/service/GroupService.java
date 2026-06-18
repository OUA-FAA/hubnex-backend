package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.GroupRequestDto;
import com.hubnex.backend.dto.response.GroupResponseDto;
import com.hubnex.backend.model.Group;
import com.hubnex.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final Set<String> ALLOWED_ACTIONS = Set.of("CREATE", "EDIT", "VIEW", "DELETE");
    private static final Set<String> ALLOWED_MODULES = Set.of(
            "USERS",
            "GROUPS",
            "COMPANIES",
            "AGENCIES",
            "HUBS",
            "CITIES",
            "MANIFESTE",
            "RECEPTION",
            "DISPATCH",
            "EXPEDITION",
            "TRACKING",
            "ROLES_PERMISSIONS"
    );

    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<GroupResponseDto> getAll() {
        return groupRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    @Transactional
    public GroupResponseDto create(GroupRequestDto dto) {
        validateNameForCreate(dto.getName());

        Group group = Group.builder()
                .name(dto.getName())
                .permissionActions(normalizePermissionValues(dto.getPermissionActions(), ALLOWED_ACTIONS, "permissionActions"))
                .permissionModules(normalizePermissionValues(dto.getPermissionModules(), ALLOWED_MODULES, "permissionModules"))
                .build();

        return mapToResponse(groupRepository.save(group));
    }

    @Transactional
    public GroupResponseDto update(Long id, GroupRequestDto dto) {
        Group group = getEntityById(id);
        validateNameForUpdate(id, dto.getName());

        group.setName(dto.getName());
        group.setPermissionActions(normalizePermissionValues(dto.getPermissionActions(), ALLOWED_ACTIONS, "permissionActions"));
        group.setPermissionModules(normalizePermissionValues(dto.getPermissionModules(), ALLOWED_MODULES, "permissionModules"));

        return mapToResponse(groupRepository.save(group));
    }

    @Transactional
    public GroupResponseDto patch(Long id, GroupRequestDto dto) {
        Group group = getEntityById(id);

        if (dto.getName() != null) {
            validateNameForUpdate(id, dto.getName());
            group.setName(dto.getName());
        }
        if (dto.getPermissionActions() != null) {
            group.setPermissionActions(normalizePermissionValues(dto.getPermissionActions(), ALLOWED_ACTIONS, "permissionActions"));
        }
        if (dto.getPermissionModules() != null) {
            group.setPermissionModules(normalizePermissionValues(dto.getPermissionModules(), ALLOWED_MODULES, "permissionModules"));
        }

        return mapToResponse(groupRepository.save(group));
    }

    @Transactional
    public void delete(Long id) {
        groupRepository.delete(getEntityById(id));
    }

    private Group getEntityById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private void validateNameForCreate(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Group name is required");
        }
        if (groupRepository.existsByName(name)) {
            throw new RuntimeException("Group name already exists");
        }
    }

    private void validateNameForUpdate(Long groupId, String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Group name is required");
        }
        groupRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(groupId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Group name already exists");
                });
    }


    private Set<String> normalizePermissionValues(Set<String> values, Set<String> allowedValues, String fieldName) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new RuntimeException(fieldName + " must not contain blank values");
            }

            String normalized = value.trim().toUpperCase();
            if (!allowedValues.contains(normalized)) {
                throw new RuntimeException("Invalid " + fieldName + " value: " + value);
            }
            normalizedValues.add(normalized);
        }

        return normalizedValues;
    }

    private GroupResponseDto mapToResponse(Group group) {
        Set<String> permissionActions = group.getPermissionActions() != null ? group.getPermissionActions() : Set.of();
        Set<String> permissionModules = group.getPermissionModules() != null ? group.getPermissionModules() : Set.of();

        return GroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .permissionActions(new LinkedHashSet<>(permissionActions))
                .permissionModules(new LinkedHashSet<>(permissionModules))
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
