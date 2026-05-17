package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.UserRequestDto;
import com.hubnex.backend.dto.response.UserResponseDto;
import com.hubnex.backend.model.Agency;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.model.Role;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.AgencyRepository;
import com.hubnex.backend.repository.HubRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final HubRepository hubRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public List<UserResponseDto> getByHubId(Long hubId) {
        return userRepository.findByHub_Id(hubId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<UserResponseDto> getByAgenceId(Long agenceId) {
        return userRepository.findByAgence_Id(agenceId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<UserResponseDto> getByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponseDto create(UserRequestDto dto) {
        validatePasswordForCreate(dto.getMotDePasse());
        validateUniqueForCreate(dto.getLogin(), dto.getEmail());

        User user = User.builder()
                .login(dto.getLogin())
                .motDePasse(passwordEncoder.encode(dto.getMotDePasse()))
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .role(dto.getRole())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .agence(resolveAgency(dto.getAgenceId()))
                .hub(resolveHub(dto.getHubId()))
                .token(dto.getToken())
                .build();

        validateRoleAttachment(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = getEntityById(id);
        validateEmailUniqueForUpdate(id, dto.getEmail());

        if (dto.getMotDePasse() != null) {
            user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }
        user.setNomComplet(dto.getNomComplet());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        user.setRole(dto.getRole());
        user.setActif(dto.getActif() != null ? dto.getActif() : user.getActif());
        user.setAgence(resolveAgency(dto.getAgenceId()));
        user.setHub(resolveHub(dto.getHubId()));
        user.setToken(dto.getToken());

        validateRoleAttachment(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto patch(Long id, UserRequestDto dto) {
        User user = getEntityById(id);

        if (dto.getMotDePasse() != null) {
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
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getActif() != null) {
            user.setActif(dto.getActif());
        }
        if (dto.getAgenceId() != null) {
            user.setAgence(resolveAgency(dto.getAgenceId()));
        }
        if (dto.getHubId() != null) {
            user.setHub(resolveHub(dto.getHubId()));
        }
        if (dto.getToken() != null) {
            user.setToken(dto.getToken());
        }

        validateRoleAttachment(user);
        return mapToResponse(userRepository.save(user));
    }

    public UserResponseDto delete(Long id) {
        User user = getEntityById(id);
        user.setActif(false);
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

    private void validateUniqueForCreate(String login, String email) {
        if (userRepository.existsByLogin(login)) {
            throw new RuntimeException("Login already exists");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
    }

    private void validatePasswordForCreate(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new RuntimeException("motDePasse is required");
        }
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

    private void validateRoleAttachment(User user) {
        if (user.getRole() == Role.AGENT_AGENCE && user.getAgence() == null) {
            throw new RuntimeException("agenceId is required for role AGENT_AGENCE");
        }
        if (user.getRole() == Role.AGENT_HUB && user.getHub() == null) {
            throw new RuntimeException("hubId is required for role AGENT_HUB");
        }
    }

    private UserResponseDto mapToResponse(User user) {
        Agency agence = user.getAgence();
        Hub hub = user.getHub();

        return UserResponseDto.builder()
                .id(user.getId())
                .login(user.getLogin())
                .nomComplet(user.getNomComplet())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .actif(user.getActif())
                .agenceId(agence != null ? agence.getId() : null)
                .agenceNom(agence != null ? agence.getNom() : null)
                .hubId(hub != null ? hub.getId() : null)
                .hubNom(hub != null ? hub.getNom() : null)
                .token(user.getToken())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
