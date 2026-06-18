package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAgencyAssignmentDto {
    private Long id;
    private String nom;
    private Long hubId;
    private String hubName;
}
