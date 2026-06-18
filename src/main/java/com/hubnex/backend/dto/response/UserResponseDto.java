package com.hubnex.backend.dto.response;

import com.hubnex.backend.model.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String login;
    private String nomComplet;
    private String email;
    private String telephone;
    private String photoUrl;
    private Role role;
    private Long roleId;
    private String roleName;
    private List<UserGroupAccessResponseDto> groups;
    private List<AccessRightResponseDto> accessRights;
    private Set<String> permissions;
    private Boolean actif;
    private Long agenceId;
    private String agenceNom;
    private Long hubId;
    private String hubNom;
    private Set<Long> agenceIds;
    private Set<String> agenceNoms;
    private Set<Long> hubIds;
    private Set<String> hubNoms;
    private Set<Long> agencyIds;
    private Set<Long> cityIds;
    private Set<String> cityNames;
    private List<UserHubAssignmentDto> hubs;
    private List<UserAgencyAssignmentDto> agencies;
    private List<UserCityAssignmentDto> cities;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
