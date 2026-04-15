package com.rayhank.tech_assesment.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String message;
    private UserTokenDto user;
}
