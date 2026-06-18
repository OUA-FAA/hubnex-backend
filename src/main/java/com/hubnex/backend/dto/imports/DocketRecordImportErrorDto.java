package com.hubnex.backend.dto.imports;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordImportErrorDto {
    private int rowNumber;
    private String field;
    private String message;
}
