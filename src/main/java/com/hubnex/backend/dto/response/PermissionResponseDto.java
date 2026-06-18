package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponseDto {
    private Long id;
    private String code;
    private String label;
    private String module;
    private String description;
}
