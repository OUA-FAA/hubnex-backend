package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.TrackingRequestDto;
import com.hubnex.backend.dto.response.TrackingResponseDto;
import com.hubnex.backend.model.DocketRecord;
import com.hubnex.backend.model.Hub;
import com.hubnex.backend.model.Tracking;
import com.hubnex.backend.model.TypeFlux;
import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.DocketRecordRepository;
import com.hubnex.backend.repository.TrackingRepository;
import com.hubnex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.hubnex.backend.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingRepository trackingRepository;
    private final UserRepository userRepository;
    private final DocketRecordRepository docketRecordRepository;

    @Transactional(readOnly = true)
    public List<TrackingResponseDto> getAll() {
        return trackingRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TrackingResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    @Transactional
    public TrackingResponseDto create(TrackingRequestDto dto) {
        Tracking tracking = createEntity(dto.getStatut(), dto.getNote(), dto.getDateScan(),
                dto.getUtilisateurId(), resolveDocketRecordId(dto.getDocketRecordId(), dto.getColisId()));
        return mapToResponse(tracking);
    }

    @Transactional
    public Tracking createEntity(String statut, String note, LocalDateTime dateScan, Long utilisateurId, Long docketRecordId) {
        Tracking tracking = Tracking.builder()
                .statut(statut)
                .note(note)
                .dateScan(dateScan != null ? dateScan : LocalDateTime.now())
                .utilisateur(getUser(utilisateurId))
                .docketRecord(docketRecordId != null ? getDocketRecord(docketRecordId) : null)
                .build();
        return trackingRepository.save(tracking);
    }

    @Transactional
    public Tracking createEvent(DocketRecord docketRecord, String action, String description) {
        return createEvent(docketRecord, action, description, null);
    }

    @Transactional
    public Tracking createEvent(DocketRecord docketRecord, String action, String description, TypeFlux typeFlux) {
        if (docketRecord == null || docketRecord.getId() == null) {
            throw new IllegalArgumentException("DocketRecord sauvegarde obligatoire pour creer un tracking");
        }

        String note = typeFlux != null
                ? description + " (typeFlux: " + typeFlux.name() + ")"
                : description;
        if (trackingRepository.existsByDocketRecordIdAndStatutAndNote(docketRecord.getId(), action, note)) {
            return null;
        }

        Tracking tracking = Tracking.builder()
                .statut(action)
                .note(note)
                .dateScan(LocalDateTime.now())
                .utilisateur(getCurrentUser())
                .docketRecord(docketRecord)
                .build();
        return trackingRepository.save(tracking);
    }

    private Tracking getEntityById(Long id) {
        return trackingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracking not found"));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur authentifie introuvable pour le tracking");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        return userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable pour le tracking"));
    }

    private Long resolveDocketRecordId(Long docketRecordId, Long legacyColisId) {
        return docketRecordId != null ? docketRecordId : legacyColisId;
    }

    private DocketRecord getDocketRecord(Long id) {
        return docketRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Docket record not found"));
    }

    public TrackingResponseDto mapToResponse(Tracking tracking) {
        User user = tracking.getUtilisateur();
        DocketRecord docketRecord = tracking.getDocketRecord();
        Hub hub = docketRecord != null ? docketRecord.getHubPrincipal() : null;
        return TrackingResponseDto.builder()
                .id(tracking.getId())
                .statut(tracking.getStatut())
                .action(tracking.getStatut())
                .note(tracking.getNote())
                .description(tracking.getNote())
                .dateScan(tracking.getDateScan())
                .dateAction(tracking.getDateScan())
                .utilisateurId(user != null ? user.getId() : null)
                .utilisateurNom(user != null ? user.getNomComplet() : null)
                .docketRecordId(docketRecord != null ? docketRecord.getId() : null)
                .docketRecordConnote(docketRecord != null ? docketRecord.getConnote() : null)
                .connote(docketRecord != null ? docketRecord.getConnote() : null)
                .hubId(hub != null ? hub.getId() : null)
                .hubNom(hub != null ? hub.getNom() : null)
                .localisation(hub != null ? hub.getNom() : null)
                .colisId(docketRecord != null ? docketRecord.getId() : null)
                .colisConnote(docketRecord != null ? docketRecord.getConnote() : null)
                .createdAt(tracking.getCreatedAt())
                .build();
    }
}
