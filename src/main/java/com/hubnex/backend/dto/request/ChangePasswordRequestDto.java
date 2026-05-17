package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequestDto {

    @NotBlank
    private String ancienMotDePasse;

    @NotBlank
    private String nouveauMotDePasse;
}
