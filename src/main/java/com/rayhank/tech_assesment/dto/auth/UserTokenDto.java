package com.rayhank.tech_assesment.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTokenDto {
    private String name;
    private String email;
    private String accessToken;
}
