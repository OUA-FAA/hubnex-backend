package com.hubnex.backend.dto.imports;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityImportErrorDto {
    private int row;
    private String message;
}
