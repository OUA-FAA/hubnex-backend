package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityResponseDto {
    private Long id;
    private String name;
    private String code;
    private Boolean active;
    private Long agenceId;
    private String agenceNom;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
