package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.UserRequestDto;
import com.hubnex.backend.dto.response.UserAgencyAssignmentDto;
import com.hubnex.backend.dto.response.UserCityAssignmentDto;
import com.hubnex.backend.dto.response.UserHubAssignmentDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.City;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.model.Role;
import com.hubnex.backend.model.RoleEntity;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.CityRepository;
import com.hubnex.backend.repository.HubRepository;
import com.hubnex.backend.repository.RoleRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private static final String PASSWORD_VALIDATION_MESSAGE =
            "Le mot de passe doit contenir au moins une lettre, un chiffre et un caractère spécial.";
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile(".*[^A-Za-z0-9\\s].*");
    private static final String PROTECTED_USER_MESSAGE =
            "Vous n'avez pas le droit d'acceder a un compte administrateur ou systeme.";
    private static final String SUP_ADMIN_ASSIGNMENT_MESSAGE =
            "Sup-Admin role cannot be assigned from the Users module.";
    private static final String ADMIN = "ADMIN";
    private static final String SUP_ADMIN_DISPLAY_NAME = "Sup-Admin";
    private static final Set<String> SYSTEM_ROLE_NAMES = Set.of(ADMIN);

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final CityRepository cityRepository;
    private final HubRepository hubRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPhotoService userPhotoService;
    private final RoleAccessService roleAccessService;

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        List<User> users = userRepository.findAll();
        boolean admin = isCurrentUserAdmin();
        List<User> visibleUsers = admin
                ? users
                : users.stream().filter(user -> !isProtectedSystemUser(user)).toList();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Users list filtering user={} isAdmin={} before={} after={}",
                authentication != null ? authentication.getName() : "anonymous",
                admin,
                users.size(),
                visibleUsers.size());

        return visibleUsers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        User user = getEntityById(id);
        ensureCanAccessUser(user);
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getAuthenticatedProfile(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public List<UserResponseDto> getByHubId(Long hubId) {
        return mergeUsers(userRepository.findByHub_Id(hubId), userRepository.findByHubs_Id(hubId)).stream()
                .filter(this::canCurrentUserAccess)
                .map(this::mapToResponse)
                .toList();
    }

    public List<UserResponseDto> getByAgenceId(Long agenceId) {
        return mergeUsers(userRepository.findByAgence_Id(agenceId), userRepository.findByAgences_Id(agenceId)).stream()
                .filter(this::canCurrentUserAccess)
                .map(this::mapToResponse)
                .toList();
    }

    public List<UserResponseDto> getByRole(String roleName) {
        return mergeUsers(findByLegacyRole(roleName), userRepository.findByRoleEntity_Name(roleName)).stream()
                .filter(this::canCurrentUserAccess)
                .map(this::mapToResponse)
                .toList();
    }

    private List<User> findByLegacyRole(String roleName) {
        try {
            return userRepository.findByRole(Role.valueOf(roleName));
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    public UserResponseDto create(UserRequestDto dto) {
        validatePasswordForCreate(dto.getMotDePasse());
        validateUniqueForCreate(dto.getLogin(), dto.getEmail());
        RoleEntity roleEntity = resolveRoleForCreate(dto);
        ensureCanAssignRole(roleEntity);

        User user = User.builder()
                .login(dto.getLogin())
                .motDePasse(passwordEncoder.encode(dto.getMotDePasse()))
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .role(resolveLegacyRoleOrNull(roleEntity))
                .roleEntity(roleEntity)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .token(dto.getToken())
                .build();

        applyHubAssignments(user, normalizeHubIds(dto));
        applyAgenceAssignments(user, normalizeAgenceIds(dto));
        applyCityAssignments(user, normalizeCityIds(dto));
        validateAssignmentHierarchy(user);

        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = getEntityById(id);
        ensureCanManageUser(user);
        validateEmailUniqueForUpdate(id, dto.getEmail());

        if (isPasswordProvided(dto.getMotDePasse())) {
            validatePasswordStrength(dto.getMotDePasse());
            user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }
        user.setNomComplet(dto.getNomComplet());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        if (isRoleProvided(dto)) {
            RoleEntity roleEntity = resolveRoleForUpdate(dto);
            ensureCanAssignRole(roleEntity);
            user.setRoleEntity(roleEntity);
            user.setRole(resolveLegacyRoleOrNull(roleEntity));
        }
        user.setActif(dto.getActif() != null ? dto.getActif() : user.getActif());
        user.setToken(dto.getToken());
        applyHubAssignments(user, normalizeHubIdsForUpdate(dto));
        applyAgenceAssignments(user, normalizeAgenceIdsForUpdate(dto));
        applyCityAssignments(user, normalizeCityIdsForUpdate(dto));
        validateAssignmentHierarchy(user);

        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto patch(Long id, UserRequestDto dto) {
        User user = getEntityById(id);
        ensureCanManageUser(user);

        if (isPasswordProvided(dto.getMotDePasse())) {
            validatePasswordStrength(dto.getMotDePasse());
            user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }
        if (dto.getNomComplet() != null) {
            user.setNomComplet(dto.getNomComplet());
        }
        if (dto.getEmail() != null) {
            validateEmailUniqueForUpdate(user.getId(), dto.getEmail());
            user.setEmail(dto.getEmail());
        }
        if (dto.getTelephone() != null) {
            user.setTelephone(dto.getTelephone());
        }
        if (isRoleProvided(dto)) {
            RoleEntity roleEntity = resolveRoleForUpdate(dto);
            ensureCanAssignRole(roleEntity);
            user.setRoleEntity(roleEntity);
            user.setRole(resolveLegacyRoleOrNull(roleEntity));
        }
        if (dto.getActif() != null) {
            user.setActif(dto.getActif());
        }
        if (isHubAssignmentProvided(dto)) {
            applyHubAssignments(user, normalizeHubIds(dto));
        }
        if (isAgenceAssignmentProvided(dto)) {
            applyAgenceAssignments(user, normalizeAgenceIds(dto));
        }
        if (isCityAssignmentProvided(dto)) {
            applyCityAssignments(user, normalizeCityIds(dto));
        }
        if (dto.getToken() != null) {
            user.setToken(dto.getToken());
        }
        validateAssignmentHierarchy(user);

        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto delete(Long id) {
        User user = getEntityById(id);
        ensureCanManageUser(user);
        user.setActif(false);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto uploadPhoto(Long id, MultipartFile file) {
        User user = getEntityById(id);
        ensureCanManageUser(user);
        userPhotoService.deleteUserPhoto(user.getPhotoUrl());
        user.setPhotoUrl(userPhotoService.storeUserPhoto(user.getId(), file));
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto deletePhoto(Long id) {
        User user = getEntityById(id);
        ensureCanManageUser(user);
        userPhotoService.deleteUserPhoto(user.getPhotoUrl());
        user.setPhotoUrl(null);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto addHub(Long userId, Long hubId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        Hub hub = resolveHub(hubId);
        ensureHubSet(user).add(hub);
        syncPrimaryHub(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto removeHub(Long userId, Long hubId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        ensureHubSet(user).removeIf(hub -> hub.getId().equals(hubId));
        syncPrimaryHub(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto addAgence(Long userId, Long agenceId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        Agency agence = resolveAgency(agenceId);
        ensureAgenceSet(user).add(agence);
        syncPrimaryAgence(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto removeAgence(Long userId, Long agenceId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        ensureAgenceSet(user).removeIf(agence -> agence.getId().equals(agenceId));
        syncPrimaryAgence(user);
        validateAssignmentHierarchy(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto addCity(Long userId, Long cityId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        City city = resolveCity(cityId);
        ensureCitySet(user).add(city);
        validateAssignmentHierarchy(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto removeCity(Long userId, Long cityId) {
        User user = getEntityById(userId);
        ensureCanManageUser(user);
        ensureCitySet(user).removeIf(city -> city.getId().equals(cityId));
        return mapToResponse(userRepository.save(user));
    }

    private User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Agency resolveAgency(Long agencyId) {
        if (agencyId == null) {
            return null;
        }
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));
    }

    private Hub resolveHub(Long hubId) {
        if (hubId == null) {
            return null;
        }
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    private City resolveCity(Long cityId) {
        if (cityId == null) {
            return null;
        }
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new RuntimeException("City not found"));
    }

    private RoleEntity resolveRoleForCreate(UserRequestDto dto) {
        if (!isRoleProvided(dto)) {
            throw new RuntimeException("roleId or roleName is required");
        }
        return resolveRoleForUpdate(dto);
    }

    private RoleEntity resolveRoleForUpdate(UserRequestDto dto) {
        if (dto.getRoleId() != null) {
            return roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
        }
        String roleName = resolveRequestedRoleName(dto);
        if (roleName == null || roleName.isBlank()) {
            throw new RuntimeException("roleId or roleName is required");
        }
        return roleRepository.findByName(roleName.trim())
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    private String resolveRequestedRoleName(UserRequestDto dto) {
        if (dto.getRoleName() != null && !dto.getRoleName().isBlank()) {
            return normalizeRequestedRoleName(dto.getRoleName());
        }
        return normalizeRequestedRoleName(dto.getRole());
    }

    private String normalizeRequestedRoleName(String roleName) {
        if (roleName == null) {
            return null;
        }
        String normalized = roleName.trim();
        if (isSupAdminRequestedName(normalized)) {
            return ADMIN;
        }
        return normalized;
    }

    private boolean isRoleProvided(UserRequestDto dto) {
        return dto.getRoleId() != null
                || (dto.getRoleName() != null && !dto.getRoleName().isBlank())
                || (dto.getRole() != null && !dto.getRole().isBlank());
    }

    private Role resolveLegacyRoleOrNull(RoleEntity roleEntity) {
        try {
            return Role.valueOf(roleEntity.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean canCurrentUserAccess(User user) {
        return isCurrentUserAdmin() || !isProtectedSystemUser(user);
    }

    private void ensureCanAccessUser(User user) {
        if (!canCurrentUserAccess(user)) {
            throw forbiddenProtectedUser();
        }
    }

    private void ensureCanManageUser(User user) {
        ensureCanAccessUser(user);
    }

    private void ensureCanAssignRole(RoleEntity roleEntity) {
        if (isSupAdminRole(roleEntity)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SUP_ADMIN_ASSIGNMENT_MESSAGE);
        }
        if (!isCurrentUserAdmin() && isProtectedSystemRole(roleEntity)) {
            throw forbiddenProtectedUser();
        }
    }

    private boolean isProtectedSystemUser(User user) {
        if (user.getRole() != null && SYSTEM_ROLE_NAMES.contains(user.getRole().name())) {
            return true;
        }
        return isProtectedSystemRole(user.getRoleEntity());
    }

    private boolean isProtectedSystemRole(RoleEntity roleEntity) {
        return roleEntity != null
                && (Boolean.TRUE.equals(roleEntity.getSystemRole())
                || isSystemRoleName(roleEntity.getName()));
    }

    private boolean isSupAdminRole(RoleEntity roleEntity) {
        return roleEntity != null
                && (isSupAdminRequestedName(roleEntity.getName())
                || isSupAdminRequestedName(roleEntity.getDescription()));
    }

    private boolean isSupAdminRequestedName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return ADMIN.equals(normalized)
                || "ROLE_ADMIN".equals(normalized)
                || "SUP_ADMIN".equals(normalized);
    }

    private boolean isSystemRoleName(String roleName) {
        return roleName != null
                && SYSTEM_ROLE_NAMES.contains(roleName.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority) || "ADMIN".equals(authority));
    }

    private ResponseStatusException forbiddenProtectedUser() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, PROTECTED_USER_MESSAGE);
    }

    private Set<Hub> resolveHubs(Set<Long> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        validateNoNullIds(hubIds, "hubIds");
        List<Hub> hubs = hubRepository.findAllById(hubIds);
        if (hubs.size() != hubIds.size()) {
            throw new RuntimeException("One or more hubs were not found");
        }
        return new LinkedHashSet<>(hubs);
    }

    private Set<Agency> resolveAgences(Set<Long> agenceIds) {
        if (agenceIds == null || agenceIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        validateNoNullIds(agenceIds, "agenceIds");
        List<Agency> agences = agencyRepository.findAllById(agenceIds);
        if (agences.size() != agenceIds.size()) {
            throw new RuntimeException("One or more agences were not found");
        }
        return new LinkedHashSet<>(agences);
    }

    private Set<City> resolveCities(Set<Long> cityIds) {
        if (cityIds == null || cityIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        validateNoNullIds(cityIds, "cityIds");
        List<City> cities = cityRepository.findAllById(cityIds);
        if (cities.size() != cityIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more cities were not found");
        }
        return new LinkedHashSet<>(cities);
    }

    private void applyHubAssignments(User user, Set<Long> hubIds) {
        user.setHubs(resolveHubs(hubIds));
        syncPrimaryHub(user);
    }

    private void applyAgenceAssignments(User user, Set<Long> agenceIds) {
        user.setAgences(resolveAgences(agenceIds));
        syncPrimaryAgence(user);
    }

    private void applyCityAssignments(User user, Set<Long> cityIds) {
        user.setCities(resolveCities(cityIds));
    }

    private void syncPrimaryHub(User user) {
        user.setHub(ensureHubSet(user).stream().findFirst().orElse(null));
    }

    private void syncPrimaryAgence(User user) {
        user.setAgence(ensureAgenceSet(user).stream().findFirst().orElse(null));
    }

    private Set<Hub> ensureHubSet(User user) {
        if (user.getHubs() == null) {
            user.setHubs(new LinkedHashSet<>());
        }
        return user.getHubs();
    }

    private Set<Agency> ensureAgenceSet(User user) {
        if (user.getAgences() == null) {
            user.setAgences(new LinkedHashSet<>());
        }
        return user.getAgences();
    }

    private Set<City> ensureCitySet(User user) {
        if (user.getCities() == null) {
            user.setCities(new LinkedHashSet<>());
        }
        return user.getCities();
    }

    private Set<Long> normalizeHubIds(UserRequestDto dto) {
        if (dto.getHubIds() != null) {
            return new LinkedHashSet<>(dto.getHubIds());
        }
        if (dto.getHubId() != null) {
            return new LinkedHashSet<>(Set.of(dto.getHubId()));
        }
        return null;
    }

    private Set<Long> normalizeAgenceIds(UserRequestDto dto) {
        if (dto.getAgencyIds() != null) {
            return new LinkedHashSet<>(dto.getAgencyIds());
        }
        if (dto.getAgenceIds() != null) {
            return new LinkedHashSet<>(dto.getAgenceIds());
        }
        if (dto.getAgenceId() != null) {
            return new LinkedHashSet<>(Set.of(dto.getAgenceId()));
        }
        return null;
    }

    private Set<Long> normalizeCityIds(UserRequestDto dto) {
        if (dto.getCityIds() != null) {
            return new LinkedHashSet<>(dto.getCityIds());
        }
        if (dto.getCityId() != null) {
            return new LinkedHashSet<>(Set.of(dto.getCityId()));
        }
        return null;
    }

    private Set<Long> normalizeHubIdsForUpdate(UserRequestDto dto) {
        Set<Long> ids = normalizeHubIds(dto);
        return ids != null ? ids : new LinkedHashSet<>();
    }

    private Set<Long> normalizeAgenceIdsForUpdate(UserRequestDto dto) {
        Set<Long> ids = normalizeAgenceIds(dto);
        return ids != null ? ids : new LinkedHashSet<>();
    }

    private Set<Long> normalizeCityIdsForUpdate(UserRequestDto dto) {
        Set<Long> ids = normalizeCityIds(dto);
        return ids != null ? ids : new LinkedHashSet<>();
    }

    private boolean isHubAssignmentProvided(UserRequestDto dto) {
        return dto.getHubIds() != null || dto.getHubId() != null;
    }

    private boolean isAgenceAssignmentProvided(UserRequestDto dto) {
        return dto.getAgencyIds() != null || dto.getAgenceIds() != null || dto.getAgenceId() != null;
    }

    private boolean isCityAssignmentProvided(UserRequestDto dto) {
        return dto.getCityIds() != null || dto.getCityId() != null;
    }

    private void validateAssignmentHierarchy(User user) {
        Set<Hub> hubs = ensureHubSet(user);
        Set<Agency> agences = ensureAgenceSet(user);
        Set<City> cities = ensureCitySet(user);

        if (!hubs.isEmpty() && !agences.isEmpty()) {
            Set<Long> hubIds = hubs.stream().map(Hub::getId).collect(java.util.stream.Collectors.toSet());
            agences.stream()
                    .filter(agence -> agence.getHub() == null || !hubIds.contains(agence.getHub().getId()))
                    .findFirst()
                    .ifPresent(agence -> {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Selected agency must belong to one of the selected hubs: " + agence.getNom());
                    });
        }

        if (!agences.isEmpty() && !cities.isEmpty()) {
            Set<Long> agenceIds = agences.stream().map(Agency::getId).collect(java.util.stream.Collectors.toSet());
            cities.stream()
                    .filter(city -> city.getAgence() == null || !agenceIds.contains(city.getAgence().getId()))
                    .findFirst()
                    .ifPresent(city -> {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Selected city must belong to one of the selected agencies: " + city.getName());
                    });
        }
    }

    private void validateNoNullIds(Set<Long> ids, String fieldName) {
        if (ids.stream().anyMatch(id -> id == null)) {
            throw new RuntimeException(fieldName + " must not contain null values");
        }
    }

    private void validateUniqueForCreate(String login, String email) {
        if (userRepository.existsByLogin(login)) {
            throw new RuntimeException("Login already exists");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
    }

    private void validatePasswordForCreate(String motDePasse) {
        validatePasswordStrength(motDePasse);
    }

    private void validatePasswordStrength(String motDePasse) {
        if (motDePasse == null
                || motDePasse.isBlank()
                || !LETTER_PATTERN.matcher(motDePasse).matches()
                || !DIGIT_PATTERN.matcher(motDePasse).matches()
                || !SPECIAL_CHARACTER_PATTERN.matcher(motDePasse).matches()) {
            throw new RuntimeException(PASSWORD_VALIDATION_MESSAGE);
        }
    }

    private boolean isPasswordProvided(String motDePasse) {
        return motDePasse != null && !motDePasse.isBlank();
    }

    private void validateEmailUniqueForUpdate(Long userId, String email) {
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Email already exists");
                });
    }

    private List<User> mergeUsers(List<User> legacyUsers, List<User> assignedUsers) {
        Map<Long, User> users = new LinkedHashMap<>();
        legacyUsers.forEach(user -> users.put(user.getId(), user));
        assignedUsers.forEach(user -> users.put(user.getId(), user));
        return users.values().stream().toList();
    }

    private UserResponseDto mapToResponse(User user) {
        Map<Long, String> agences = collectAgenceInfo(user);
        Map<Long, String> hubs = collectHubInfo(user);
        Map<Long, String> cities = collectCityInfo(user);
        Agency primaryAgence = user.getAgence();
        Hub primaryHub = user.getHub();
        RoleEntity roleEntity = user.getRoleEntity();
        List<UserHubAssignmentDto> hubAssignments = mapHubAssignments(user);
        List<UserAgencyAssignmentDto> agencyAssignments = mapAgencyAssignments(user);
        List<UserCityAssignmentDto> cityAssignments = mapCityAssignments(user);

        return UserResponseDto.builder()
                .id(user.getId())
                .login(user.getLogin())
                .nomComplet(user.getNomComplet())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .photoUrl(user.getPhotoUrl())
                .role(user.getRole())
                .roleId(roleEntity != null ? roleEntity.getId() : null)
                .roleName(resolveResponseRoleName(user))
                .groups(roleAccessService.resolveGroups(user))
                .accessRights(roleAccessService.resolveAccessRights(user))
                .permissions(roleEntity != null
                        ? roleAccessService.resolveFinalPermissions(roleEntity)
                        : Set.of())
                .actif(user.getActif())
                .agenceId(primaryAgence != null ? primaryAgence.getId() : firstKey(agences))
                .agenceNom(primaryAgence != null ? primaryAgence.getNom() : firstValue(agences))
                .hubId(primaryHub != null ? primaryHub.getId() : firstKey(hubs))
                .hubNom(primaryHub != null ? primaryHub.getNom() : firstValue(hubs))
                .agenceIds(new LinkedHashSet<>(agences.keySet()))
                .agenceNoms(new LinkedHashSet<>(agences.values()))
                .hubIds(new LinkedHashSet<>(hubs.keySet()))
                .hubNoms(new LinkedHashSet<>(hubs.values()))
                .agencyIds(new LinkedHashSet<>(agences.keySet()))
                .cityIds(new LinkedHashSet<>(cities.keySet()))
                .cityNames(new LinkedHashSet<>(cities.values()))
                .hubs(hubAssignments)
                .agencies(agencyAssignments)
                .cities(cityAssignments)
                .token(user.getToken())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private Map<Long, String> collectAgenceInfo(User user) {
        Map<Long, String> agences = new LinkedHashMap<>();
        addAgenceInfo(agences, user.getAgence());
        if (user.getAgences() != null) {
            user.getAgences().forEach(agence -> addAgenceInfo(agences, agence));
        }
        return agences;
    }

    private Map<Long, String> collectHubInfo(User user) {
        Map<Long, String> hubs = new LinkedHashMap<>();
        addHubInfo(hubs, user.getHub());
        if (user.getHubs() != null) {
            user.getHubs().forEach(hub -> addHubInfo(hubs, hub));
        }
        return hubs;
    }

    private Map<Long, String> collectCityInfo(User user) {
        Map<Long, String> cities = new LinkedHashMap<>();
        if (user.getCities() != null) {
            user.getCities().forEach(city -> addCityInfo(cities, city));
        }
        return cities;
    }

    private void addAgenceInfo(Map<Long, String> agences, Agency agence) {
        if (agence != null) {
            agences.put(agence.getId(), agence.getNom());
        }
    }

    private void addHubInfo(Map<Long, String> hubs, Hub hub) {
        if (hub != null) {
            hubs.put(hub.getId(), hub.getNom());
        }
    }

    private void addCityInfo(Map<Long, String> cities, City city) {
        if (city != null) {
            cities.put(city.getId(), city.getName());
        }
    }

    private List<UserHubAssignmentDto> mapHubAssignments(User user) {
        return collectHubInfo(user).entrySet().stream()
                .map(entry -> UserHubAssignmentDto.builder()
                        .id(entry.getKey())
                        .nom(entry.getValue())
                        .build())
                .toList();
    }

    private List<UserAgencyAssignmentDto> mapAgencyAssignments(User user) {
        return collectAssignedAgencies(user).stream()
                .map(agence -> UserAgencyAssignmentDto.builder()
                        .id(agence.getId())
                        .nom(agence.getNom())
                        .hubId(agence.getHub() != null ? agence.getHub().getId() : null)
                        .hubName(agence.getHub() != null ? agence.getHub().getNom() : null)
                        .build())
                .toList();
    }

    private List<UserCityAssignmentDto> mapCityAssignments(User user) {
        return ensureCitySet(user).stream()
                .map(city -> UserCityAssignmentDto.builder()
                        .id(city.getId())
                        .name(city.getName())
                        .agencyId(city.getAgence() != null ? city.getAgence().getId() : null)
                        .agencyName(city.getAgence() != null ? city.getAgence().getNom() : null)
                        .build())
                .toList();
    }

    private Set<Agency> collectAssignedAgencies(User user) {
        Set<Agency> agences = new LinkedHashSet<>();
        if (user.getAgence() != null) {
            agences.add(user.getAgence());
        }
        if (user.getAgences() != null) {
            agences.addAll(user.getAgences());
        }
        return agences;
    }

    private Long firstKey(Map<Long, String> values) {
        return values.keySet().stream().findFirst().orElse(null);
    }

    private String firstValue(Map<Long, String> values) {
        return values.values().stream().findFirst().orElse(null);
    }

    private String resolveResponseRoleName(User user) {
        if (user.getRoleEntity() != null && user.getRoleEntity().getName() != null) {
            return displayRoleName(user.getRoleEntity().getName());
        }
        return user.getRole() != null ? displayRoleName(user.getRole().name()) : null;
    }

    private String displayRoleName(String roleName) {
        if (roleName != null && ADMIN.equalsIgnoreCase(roleName.trim())) {
            return SUP_ADMIN_DISPLAY_NAME;
        }
        return roleName;
    }
}
