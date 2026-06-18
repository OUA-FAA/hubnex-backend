package com.hubnex.backend.dto.imports;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityExcelRowDto {
    private int rowNumber;
    private String name;
    private String code;
    private Boolean active;
    private Double latitude;
    private Double longitude;
}
