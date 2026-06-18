package com.hubnex.backend.dto.imports;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubImportResultDto {
    private int totalRows;
    private int createdCount;
    private int skippedCount;
    private List<HubImportErrorDto> errors;
}
