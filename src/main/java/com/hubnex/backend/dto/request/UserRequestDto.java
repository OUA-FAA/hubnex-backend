package com.hubnex.backend.dto.request;

import com.hubnex.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    @NotNull
    private Role role;

    private Boolean actif;
    private Long agenceId;
    private Long hubId;
    private String token;
}
