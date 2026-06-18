package com.hubnex.backend.dto.imports;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordImportResultDto {
    private int totalRows;
    private int createdCount;
    private int skippedCount;
    private List<DocketRecordImportErrorDto> errors;
    private List<DocketRecordImportFailedRowDto> failedRows;
}
