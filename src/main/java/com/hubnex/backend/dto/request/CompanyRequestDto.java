package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDto {

    @NotBlank
    private String name;

    private String code;

    @Email
    private String email;

    private String phone;
    private String address;
    private Boolean active;
}
