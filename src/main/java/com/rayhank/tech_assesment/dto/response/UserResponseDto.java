package com.rayhank.tech_assesment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;

    @JsonProperty("email_verified_at")
    private Instant emailVerifiedAt;
}
