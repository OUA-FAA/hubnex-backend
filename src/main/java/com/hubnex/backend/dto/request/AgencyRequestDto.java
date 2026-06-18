package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyRequestDto {

    @NotBlank
    private String nom;

    private String adresse;
    private String telephone;
    private String responsable;
    private Boolean active;

    @NotNull
    private Long hubId;
}
