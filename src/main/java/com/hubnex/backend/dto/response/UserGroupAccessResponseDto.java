package com.hubnex.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupAccessResponseDto {
    private Long id;
    private String name;
    private Set<String> permissionActions;
    private Set<String> permissionModules;
}
