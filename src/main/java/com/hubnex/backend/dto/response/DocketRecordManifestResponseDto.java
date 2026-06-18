package com.hubnex.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordManifestResponseDto {
    private String importBatchId;
    private String manifestName;
    private LocalDateTime importedAt;
    private Long totalRecords;
    private Map<String, Long> statusSummary;
}
