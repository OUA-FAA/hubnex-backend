package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionRecoveryResponseDto {
    private String recoveryId;
    private String receptionRecoveryId;
    private String recoveryName;
    private LocalDateTime recoveredAt;
    private Long totalRecords;
    private Long completed;
    private Long incomplete;
    private Long completedCount;
    private Long incompleteCount;
}
