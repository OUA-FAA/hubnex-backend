package com.hubnex.backend.config;

import com.hubnex.backend.model.Permission;
import com.hubnex.backend.model.RoleEntity;
import com.hubnex.backend.repository.PermissionRepository;
import com.hubnex.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RolePermissionInitializer implements CommandLineRunner {

    private static final String ADMIN = "ADMIN";
    private static final String SUP_ADMIN_DISPLAY_NAME = "Sup-Admin";
    private static final Set<String> EXPEDITION_PERMISSION_CODES = Set.of(
            "LOGISTICS_EXPEDITION_VIEW",
            "LOGISTICS_EXPEDITION_CREATE",
            "LOGISTICS_EXPEDITION_UPDATE",
            "LOGISTICS_EXPEDITION_DELETE",
            "LOGISTICS_EXPEDITION_VALIDATE",
            "BON_EXPEDITION_VIEW",
            "BON_EXPEDITION_CREATE",
            "BON_EXPEDITION_UPDATE",
            "BON_EXPEDITION_DELETE"
    );

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializePermissions();
        removeExpeditionPermissions();
        initializeRoles();
    }

    private void initializePermissions() {
        for (PermissionSeed seed : permissionSeeds()) {
            permissionRepository.findByCode(seed.code())
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .code(seed.code())
                            .label(seed.label())
                            .module(seed.module())
                            .description(seed.description())
                            .build()));
        }
    }

    private void initializeRoles() {
        Map<String, Permission> permissionsByCode = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));

        RoleEntity admin = roleRepository.findByName(ADMIN)
                .or(() -> roleRepository.findByName(SUP_ADMIN_DISPLAY_NAME))
                .orElseGet(() -> RoleEntity.builder()
                        .name(ADMIN)
                        .description("Sup-Admin")
                        .active(true)
                        .systemRole(true)
                        .build());
        admin.setName(ADMIN);
        admin.setDescription("Sup-Admin");
        admin.setPermissions(new LinkedHashSet<>(permissionsByCode.values()));
        admin.setActive(true);
        admin.setSystemRole(true);
        roleRepository.save(admin);
    }

    private void removeExpeditionPermissions() {
        List<Permission> expeditionPermissions = permissionRepository.findAll().stream()
                .filter(permission -> EXPEDITION_PERMISSION_CODES.contains(permission.getCode()))
                .toList();

        if (expeditionPermissions.isEmpty()) {
            return;
        }

        roleRepository.findAll().forEach(role -> {
            if (role.getPermissions() != null
                    && role.getPermissions().removeIf(permission -> EXPEDITION_PERMISSION_CODES.contains(permission.getCode()))) {
                roleRepository.save(role);
            }
        });

        expeditionPermissions.forEach(permissionRepository::delete);
    }

    private List<PermissionSeed> permissionSeeds() {
        return List.of(
                permission("DASHBOARD_VIEW", "Voir le dashboard", "Dashboard"),
                permission("USER_VIEW", "Voir les utilisateurs", "Users"),
                permission("USER_CREATE", "Creer un utilisateur", "Users"),
                permission("USER_UPDATE", "Modifier un utilisateur", "Users"),
                permission("USER_DELETE", "Supprimer un utilisateur", "Users"),
                permission("USER_ASSIGN_HUB", "Affecter des hubs", "Users"),
                permission("USER_ASSIGN_AGENCE", "Affecter des agences", "Users"),
                permission("USER_UPLOAD_PHOTO", "Uploader une photo utilisateur", "Users"),
                permission("GROUP_VIEW", "Voir les groupes", "Groups"),
                permission("GROUP_CREATE", "Creer un groupe", "Groups"),
                permission("GROUP_UPDATE", "Modifier un groupe", "Groups"),
                permission("GROUP_DELETE", "Supprimer un groupe", "Groups"),
                permission("GROUP_MANAGE_USERS", "Gerer les utilisateurs des groupes", "Groups"),
                permission("COMPANY_VIEW", "Voir les companies", "Companies"),
                permission("COMPANY_CREATE", "Creer une company", "Companies"),
                permission("COMPANY_UPDATE", "Modifier une company", "Companies"),
                permission("COMPANY_DELETE", "Supprimer une company", "Companies"),
                permission("CITY_VIEW", "Voir les cities", "Cities"),
                permission("CITY_CREATE", "Creer une city", "Cities"),
                permission("CITY_UPDATE", "Modifier une city", "Cities"),
                permission("CITY_DELETE", "Supprimer une city", "Cities"),
                permission("CITY_IMPORT_EXCEL", "Importer des cities depuis Excel", "Cities"),
                permission("CITY_VIEW_MAP", "Voir la carte des cities", "Cities"),
                permission("CITY_ASSIGN_AGENCE", "Affecter une city a une agence", "Cities"),
                permission("AGENCE_VIEW", "Voir les agences", "Agences"),
                permission("AGENCE_CREATE", "Creer une agence", "Agences"),
                permission("AGENCE_UPDATE", "Modifier une agence", "Agences"),
                permission("AGENCE_DELETE", "Supprimer une agence", "Agences"),
                permission("AGENCE_MANAGE_CITIES", "Gerer les cities des agences", "Agences"),
                permission("HUB_VIEW", "Voir les hubs", "Hubs"),
                permission("HUB_CREATE", "Creer un hub", "Hubs"),
                permission("HUB_UPDATE", "Modifier un hub", "Hubs"),
                permission("HUB_DELETE", "Supprimer un hub", "Hubs"),
                permission("ROLE_VIEW", "Voir les roles", "Roles & Permissions"),
                permission("ROLE_CREATE", "Creer un role", "Roles & Permissions"),
                permission("ROLE_UPDATE", "Modifier un role", "Roles & Permissions"),
                permission("ROLE_DELETE", "Supprimer un role", "Roles & Permissions"),
                permission("ROLE_MANAGE_PERMISSIONS", "Gerer les permissions des roles", "Roles & Permissions"),
                permission("PROFILE_VIEW", "Voir son profil", "Profile"),
                permission("PROFILE_UPDATE", "Modifier son profil", "Profile"),
                permission("DOCKET_RECORD_VIEW", "Voir les manifestes colis", "Logistics"),
                permission("DOCKET_RECORD_CREATE", "Creer un manifeste colis", "Logistics"),
                permission("DOCKET_RECORD_UPDATE", "Modifier un manifeste colis", "Logistics"),
                permission("DOCKET_RECORD_DELETE", "Supprimer un manifeste colis", "Logistics"),
                permission("DOCKET_RECORD_IMPORT_EXCEL", "Importer des manifestes colis depuis Excel", "Logistics"),
                permission("LOGISTICS_RECEPTION_VIEW", "Voir les receptions logistics", "Logistics"),
                permission("LOGISTICS_RECEPTION_CREATE", "Creer une reception logistics", "Logistics"),
                permission("LOGISTICS_RECEPTION_UPDATE", "Modifier une reception logistics", "Logistics"),
                permission("LOGISTICS_RECEPTION_DELETE", "Supprimer une reception logistics", "Logistics"),
                permission("LOGISTICS_RECEPTION_VALIDATE", "Valider une reception logistics", "Logistics"),
                permission("LOGISTICS_DISPATCH_VIEW", "Voir les dispatch logistics", "Logistics"),
                permission("LOGISTICS_DISPATCH_CREATE", "Creer un dispatch logistics", "Logistics"),
                permission("LOGISTICS_DISPATCH_UPDATE", "Modifier un dispatch logistics", "Logistics"),
                permission("LOGISTICS_DISPATCH_DELETE", "Supprimer un dispatch logistics", "Logistics"),
                permission("LOGISTICS_DISPATCH_VALIDATE", "Valider un dispatch logistics", "Logistics"),
                permission("EXPEDITION_VIEW", "Voir les expeditions", "Expedition"),
                permission("EXPEDITION_CREATE", "Creer une expedition", "Expedition"),
                permission("EXPEDITION_UPDATE", "Modifier une expedition", "Expedition"),
                permission("EXPEDITION_DELETE", "Supprimer une expedition", "Expedition"),
                permission("LOGISTICS_TRACKING_VIEW", "Voir le tracking logistics", "Logistics"),
                permission("LOGISTICS_MANIFESTE_VIEW", "Voir les manifestes logistics", "Logistics"),
                permission("LOGISTICS_MANIFESTE_CREATE", "Creer un manifeste logistics", "Logistics"),
                permission("LOGISTICS_MANIFESTE_UPDATE", "Modifier un manifeste logistics", "Logistics"),
                permission("LOGISTICS_MANIFESTE_DELETE", "Supprimer un manifeste logistics", "Logistics"),
                permission("ETIQUETTE_VIEW", "Voir les etiquettes", "Logistics / Etiquettes"),
                permission("ETIQUETTE_CREATE", "Creer une etiquette", "Logistics / Etiquettes"),
                permission("ETIQUETTE_UPDATE", "Modifier une etiquette", "Logistics / Etiquettes"),
                permission("ETIQUETTE_DELETE", "Supprimer une etiquette", "Logistics / Etiquettes"),
                permission("BON_RECEPTION_VIEW", "Voir les bons reception", "Logistics / Bon Reception"),
                permission("BON_RECEPTION_CREATE", "Creer un bon reception", "Logistics / Bon Reception"),
                permission("BON_RECEPTION_UPDATE", "Modifier un bon reception", "Logistics / Bon Reception"),
                permission("BON_RECEPTION_DELETE", "Supprimer un bon reception", "Logistics / Bon Reception"),
                permission("BON_RECEPTION_VALIDATE", "Valider un bon reception", "Logistics / Bon Reception"),
                permission("TRACKING_VIEW", "Voir le tracking", "Logistics / Tracking"),
                permission("TRACKING_CREATE", "Creer une ligne tracking", "Logistics / Tracking")
        );
    }

    private PermissionSeed permission(String code, String label, String module) {
        return new PermissionSeed(code, label, module, label);
    }

    private record PermissionSeed(String code, String label, String module, String description) {
    }
}
