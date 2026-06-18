package com.hubnex.backend.dto.dashboard;

import com.hubnex.backend.model.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRoleStatDto {
    private Role role;
    private long count;
}
