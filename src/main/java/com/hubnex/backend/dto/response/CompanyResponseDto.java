package com.hubnex.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDto {
    private Long id;
    private String name;
    private String code;
    private String email;
    private String phone;
    private String address;
    private Boolean active;
}
