package com.hubnex.backend.service;

import com.hubnex.backend.dto.dashboard.DashboardAgenceCitiesDto;
import com.hubnex.backend.dto.dashboard.DashboardHubAgencesDto;
import com.hubnex.backend.dto.dashboard.DashboardRecentActionDto;
import com.hubnex.backend.dto.dashboard.DashboardRoleStatDto;
import com.hubnex.backend.dto.dashboard.DashboardSummaryDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.City;
import com.hubnex.backend.model.Group;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.model.Role;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.CityRepository;
import com.hubnex.backend.repository.CompanyRepository;
import com.hubnex.backend.repository.GroupRepository;
import com.hubnex.backend.repository.HubRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_ACTION_LIMIT = 20;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final CompanyRepository companyRepository;
    private final CityRepository cityRepository;
    private final AgencyRepository agencyRepository;
    private final HubRepository hubRepository;

    public DashboardSummaryDto getSummary() {
        return DashboardSummaryDto.builder()
                .totalUsers(userRepository.count())
                .totalGroups(groupRepository.count())
                .totalCompanies(companyRepository.count())
                .totalCities(cityRepository.count())
                .totalAgences(agencyRepository.count())
                .totalHubs(hubRepository.count())
                .activeUsers(userRepository.countByActifTrue())
                .inactiveUsers(userRepository.countByActifFalse())
                .activeCities(cityRepository.countByActiveTrue())
                .inactiveCities(cityRepository.countByActiveFalse())
                .activeAgences(agencyRepository.countByActiveTrue())
                .inactiveAgences(agencyRepository.countByActiveFalse())
                .activeHubs(hubRepository.countByActifTrue())
                .inactiveHubs(hubRepository.countByActifFalse())
                .build();
    }

    public List<DashboardRoleStatDto> getUsersByRole() {
        List<DashboardRoleStatDto> stats = new ArrayList<>();
        for (Role role : Role.values()) {
            stats.add(DashboardRoleStatDto.builder()
                    .role(role)
                    .count(userRepository.countByRole(role))
                    .build());
        }
        return stats;
    }

    public List<DashboardHubAgencesDto> getAgencesByHub() {
        return hubRepository.findAll().stream()
                .map(hub -> DashboardHubAgencesDto.builder()
                        .hubId(hub.getId())
                        .hubNom(hub.getNom())
                        .agenceCount(agencyRepository.countByHub_Id(hub.getId()))
                        .build())
                .toList();
    }

    public List<DashboardAgenceCitiesDto> getCitiesByAgence() {
        return agencyRepository.findAll().stream()
                .map(agence -> DashboardAgenceCitiesDto.builder()
                        .agenceId(agence.getId())
                        .agenceNom(agence.getNom())
                        .cityCount(cityRepository.countByAgence_Id(agence.getId()))
                        .build())
                .toList();
    }

    public List<DashboardRecentActionDto> getRecentActions() {
        List<DashboardRecentActionDto> actions = new ArrayList<>();

        userRepository.findAll().forEach(user -> addUserActions(actions, user));
        cityRepository.findAll().forEach(city -> addCityActions(actions, city));
        agencyRepository.findAll().forEach(agence -> addAgencyActions(actions, agence));
        hubRepository.findAll().forEach(hub -> addHubActions(actions, hub));
        groupRepository.findAll().forEach(group -> addGroupActions(actions, group));

        return actions.stream()
                .filter(action -> action.getCreatedAt() != null)
                .sorted(Comparator.comparing(DashboardRecentActionDto::getCreatedAt).reversed())
                .limit(RECENT_ACTION_LIMIT)
                .toList();
    }

    private void addUserActions(List<DashboardRecentActionDto> actions, User user) {
        addCreatedAction(actions, "USER_CREATED", "Nouvel utilisateur créé",
                "Utilisateur " + user.getNomComplet() + " créé", "Utilisateur", user.getId(), user.getCreatedAt());
        addUpdatedAction(actions, user.getCreatedAt(), user.getUpdatedAt(), "USER_UPDATED", "Utilisateur modifié",
                "Utilisateur " + user.getNomComplet() + " modifié", "Utilisateur", user.getId());
    }

    private void addCityActions(List<DashboardRecentActionDto> actions, City city) {
        addCreatedAction(actions, "CITY_CREATED", "City ajoutée",
                "City " + city.getName() + " ajoutée", "City", city.getId(), city.getCreatedAt());
        addUpdatedAction(actions, city.getCreatedAt(), city.getUpdatedAt(), "CITY_UPDATED", "City modifiée",
                "City " + city.getName() + " modifiée", "City", city.getId());
    }

    private void addAgencyActions(List<DashboardRecentActionDto> actions, Agency agence) {
        addCreatedAction(actions, "AGENCE_CREATED", "Agence créée",
                "Agence " + agence.getNom() + " créée", "Agence", agence.getId(), agence.getCreatedAt());
        addUpdatedAction(actions, agence.getCreatedAt(), agence.getUpdatedAt(), "AGENCE_UPDATED", "Agence modifiée",
                "Agence " + agence.getNom() + " modifiée", "Agence", agence.getId());
    }

    private void addHubActions(List<DashboardRecentActionDto> actions, Hub hub) {
        addCreatedAction(actions, "HUB_CREATED", "Hub créé",
                "Hub " + hub.getNom() + " créé", "Hub", hub.getId(), hub.getCreatedAt());
        addUpdatedAction(actions, hub.getCreatedAt(), hub.getUpdatedAt(), "HUB_UPDATED", "Hub modifié",
                "Hub " + hub.getNom() + " modifié", "Hub", hub.getId());
    }

    private void addGroupActions(List<DashboardRecentActionDto> actions, Group group) {
        addCreatedAction(actions, "GROUP_CREATED", "Groupe créé",
                "Groupe " + group.getName() + " créé", "Group", group.getId(), group.getCreatedAt());
        addUpdatedAction(actions, group.getCreatedAt(), group.getUpdatedAt(), "GROUP_UPDATED", "Groupe modifié",
                "Groupe " + group.getName() + " modifié", "Group", group.getId());
    }

    private void addCreatedAction(List<DashboardRecentActionDto> actions, String type, String title,
                                  String description, String entityType, Long entityId, LocalDateTime createdAt) {
        actions.add(DashboardRecentActionDto.builder()
                .type(type)
                .title(title)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(createdAt)
                .build());
    }

    private void addUpdatedAction(List<DashboardRecentActionDto> actions, LocalDateTime createdAt,
                                  LocalDateTime updatedAt, String type, String title, String description,
                                  String entityType, Long entityId) {
        if (updatedAt == null || updatedAt.equals(createdAt)) {
            return;
        }
        actions.add(DashboardRecentActionDto.builder()
                .type(type)
                .title(title)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(updatedAt)
                .build());
    }
}
