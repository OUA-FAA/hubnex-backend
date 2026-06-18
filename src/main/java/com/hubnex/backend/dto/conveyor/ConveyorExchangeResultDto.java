package com.hubnex.backend.dto.conveyor;

import com.hubnex.backend.dto.imports.DocketRecordImportErrorDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConveyorExchangeResultDto {
    private String message;
    private int totalRows;
    private int sentCount;
    private int updatedCount;
    private int skippedCount;
    private List<DocketRecordImportErrorDto> errors;
}
