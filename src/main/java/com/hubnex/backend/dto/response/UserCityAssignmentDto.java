package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCityAssignmentDto {
    private Long id;
    private String name;
    private Long agencyId;
    private String agencyName;
}
