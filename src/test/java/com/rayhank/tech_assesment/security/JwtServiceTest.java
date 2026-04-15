package com.rayhank.tech_assesment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0hISEhISE=";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L); // 1 hour

        userDetails = new User(
                "user1@webtech.id",
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("generateToken should return a non-blank JWT string")
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername should return the email used to generate the token")
    void extractUsername_shouldReturnCorrectEmail() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token)).isEqualTo("user1@webtech.id");
    }

    @Test
    @DisplayName("isTokenValid should return true for a freshly generated token")
    void isTokenValid_withFreshToken_shouldReturnTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid should return false when token belongs to a different user")
    void isTokenValid_withDifferentUser_shouldReturnFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails anotherUser = new User(
                "user2@webtech.id", "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    @DisplayName("extractUsername should throw when token is expired")
    void extractUsername_withExpiredToken_shouldThrow() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L); // already expired
        String expiredToken = jwtService.generateToken(userDetails);

        // Restore normal expiration so the parser can still parse (but claims will show expired)
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("extractUsername should throw when token is malformed")
    void extractUsername_withMalformedToken_shouldThrow() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
