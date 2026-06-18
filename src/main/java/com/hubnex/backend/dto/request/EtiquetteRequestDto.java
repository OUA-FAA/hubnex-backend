package com.hubnex.backend.dto.request;

import com.hubnex.backend.model.EtatEtiquette;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtiquetteRequestDto {
    private String reference;
    private EtatEtiquette etat;
    @NotNull
    @Min(0)
    private Integer nombreCodes;
    private String parentBarcode;
    @NotNull
    private Long hubId;
}
