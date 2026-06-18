package com.hubnex.backend.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAgenceCitiesDto {
    private Long agenceId;
    private String agenceNom;
    private long cityCount;
}
