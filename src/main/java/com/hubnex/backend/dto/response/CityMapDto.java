package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityMapDto {
    private Long id;
    private String name;
    private String code;
    private Boolean active;
    private Long agenceId;
    private String agenceNom;
    private Double latitude;
    private Double longitude;
    private int agenceCount;
}
