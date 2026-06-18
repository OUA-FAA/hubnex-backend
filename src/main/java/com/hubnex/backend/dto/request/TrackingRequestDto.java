package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingRequestDto {
    @NotBlank
    private String statut;
    private String note;
    private LocalDateTime dateScan;
    @NotNull
    private Long utilisateurId;
    private Long docketRecordId;
    private Long colisId;
}
