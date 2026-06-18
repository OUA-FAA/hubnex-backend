package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotBlank
    private String login;

    private String motDePasse;

    @NotBlank
    private String nomComplet;

    @Email
    private String email;

    private String telephone;

    private String role;

    private Long roleId;
    private String roleName;

    private Boolean actif;
    private Long agenceId;
    private Long hubId;
    private Long cityId;
    private Set<Long> agenceIds;
    private Set<Long> hubIds;
    private Set<Long> agencyIds;
    private Set<Long> cityIds;
    private String token;
}
