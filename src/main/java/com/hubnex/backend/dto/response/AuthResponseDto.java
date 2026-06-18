package com.hubnex.backend.dto.response;

import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private UserResponseDto utilisateur;
    private Long userId;
    private String login;
    private String email;
    private Long roleId;
    private String roleName;
    private List<UserGroupAccessResponseDto> groups;
    private List<AccessRightResponseDto> accessRights;
    private Set<String> permissions;
}
