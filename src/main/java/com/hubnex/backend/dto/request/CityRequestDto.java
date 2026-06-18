package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityRequestDto {

    @NotBlank
    private String name;

    private String code;
    private Boolean active;
    private Long agenceId;
    private Double latitude;
    private Double longitude;
}
