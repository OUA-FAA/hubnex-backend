package com.hubnex.backend.dto.request;

import com.hubnex.backend.model.TypeFlux;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendToConveyorRequestDto {
    private TypeFlux typeFlux;
    private Set<String> importBatchIds;
}
