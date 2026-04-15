package com.rayhank.tech_assesment.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private String type;
    private String email;
    private String name;
}
