package com.hubnex.backend.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private long totalUsers;
    private long totalGroups;
    private long totalCompanies;
    private long totalCities;
    private long totalAgences;
    private long totalHubs;
    private long activeUsers;
    private long inactiveUsers;
    private long activeCities;
    private long inactiveCities;
    private long activeAgences;
    private long inactiveAgences;
    private long activeHubs;
    private long inactiveHubs;
}
