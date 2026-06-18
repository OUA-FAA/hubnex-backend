package com.hubnex.backend.dto.dashboard;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRecentActionDto {
    private String type;
    private String title;
    private String description;
    private String entityType;
    private Long entityId;
    private LocalDateTime createdAt;
}
