package com.hubnex.backend.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardHubAgencesDto {
    private Long hubId;
    private String hubNom;
    private long agenceCount;
}
