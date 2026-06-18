package com.hubnex.backend.dto.request;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRolePermissionsDto {
    private Set<Long> permissionIds;
}
