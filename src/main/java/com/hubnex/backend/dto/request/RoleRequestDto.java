package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequestDto {

    @NotBlank
    private String name;

    private Boolean active;

    private Set<Long> groupIds;
}
