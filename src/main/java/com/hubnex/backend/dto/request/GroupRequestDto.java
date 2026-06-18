package com.hubnex.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupRequestDto {

    @NotBlank
    private String name;

    private Set<String> permissionActions;
    private Set<String> permissionModules;
}
