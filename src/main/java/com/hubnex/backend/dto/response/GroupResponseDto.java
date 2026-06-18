package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponseDto {
    private Long id;
    private String name;
    private Set<String> permissionActions;
    private Set<String> permissionModules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
