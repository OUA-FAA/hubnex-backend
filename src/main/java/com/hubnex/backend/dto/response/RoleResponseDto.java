package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private Long id;
    private String name;
    private String technicalName;
    private String displayName;
    private String type;
    private String roleType;
    private Boolean active;
    private Boolean systemRole;
    private List<RoleGroupResponseDto> groups;
    private List<PermissionResponseDto> permissions;
    private Set<String> permissionActions;
    private Set<String> permissionModules;
    private Set<String> finalPermissions;
    private Integer permissionCount;
    private Integer coveredModuleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
