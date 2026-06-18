package com.hubnex.backend.controller;

import com.hubnex.backend.dto.dashboard.DashboardAgenceCitiesDto;
import com.hubnex.backend.dto.dashboard.DashboardHubAgencesDto;
import com.hubnex.backend.dto.dashboard.DashboardRecentActionDto;
import com.hubnex.backend.dto.dashboard.DashboardRoleStatDto;
import com.hubnex.backend.dto.dashboard.DashboardSummaryDto;
import com.hubnex.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/users-by-role")
    public List<DashboardRoleStatDto> getUsersByRole() {
        return dashboardService.getUsersByRole();
    }

    @GetMapping("/agences-by-hub")
    public List<DashboardHubAgencesDto> getAgencesByHub() {
        return dashboardService.getAgencesByHub();
    }

    @GetMapping("/cities-by-agence")
    public List<DashboardAgenceCitiesDto> getCitiesByAgence() {
        return dashboardService.getCitiesByAgence();
    }

    @GetMapping("/recent-actions")
    public List<DashboardRecentActionDto> getRecentActions() {
        return dashboardService.getRecentActions();
    }
}
