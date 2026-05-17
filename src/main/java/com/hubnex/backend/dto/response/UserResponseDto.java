package com.hubnex.backend.dto.response;

import com.hubnex.backend.model.Role;
import lombok.*;

import java.time.LocalDateTime;

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
    private Role role;
    private Boolean actif;
    private Long agenceId;
    private String agenceNom;
    private Long hubId;
    private String hubNom;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
