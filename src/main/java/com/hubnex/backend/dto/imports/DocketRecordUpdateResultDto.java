package com.hubnex.backend.dto.imports;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordUpdateResultDto {
    private int totalRows;
    private int updatedCount;
    private int skippedCount;
    private List<DocketRecordImportErrorDto> errors;
}
