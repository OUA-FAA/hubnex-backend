package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubRequestDto {

    @NotBlank
    private String nom;

    private String barcode;

    private String adresse;
    private String telephone;
    private Boolean actif;
}
