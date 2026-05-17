package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubStatsResponseDto {
    private Long hubId;
    private String hubNom;
    private Long colisTraites;
    private Long bonsOuverts;
}
