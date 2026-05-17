package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyStatsResponseDto {
    private Long agenceId;
    private String agenceNom;
    private Long colisEnAttenteLivraison;
}
