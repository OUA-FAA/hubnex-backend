package com.hubnex.backend.dto.imports;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocketRecordImportFailedRowDto {
    private int rowNumber;
    private String batchNumber;
    private String lta;
    private String connote;
    private String hub;
    private String line;
    private String weight;
    private String width;
    private String length;
    private String height;
    private String volume;
    private String dateReception;
    private String errorField;
    private String errorMessage;
}
