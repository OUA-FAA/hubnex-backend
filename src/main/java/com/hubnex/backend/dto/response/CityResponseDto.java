package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityResponseDto {
    private Long id;
    private String name;
    private String code;
    private Long agencyId;
    private String agencyNom;
}
