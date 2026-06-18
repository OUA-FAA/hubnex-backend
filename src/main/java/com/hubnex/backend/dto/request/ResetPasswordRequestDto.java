package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDto {

    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}
