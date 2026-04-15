package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.auth.LoginRequest;
import com.rayhank.tech_assesment.dto.auth.LoginResponse;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.UserRepository;
import com.rayhank.tech_assesment.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthService authService;

    private LoginRequest validRequest;
    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        validRequest = new LoginRequest();
        ReflectionTestUtils.setField(validRequest, "email", "user1@webtech.id");
        ReflectionTestUtils.setField(validRequest, "password", "password1");

        user = new User();
        user.setName("User 1");
        user.setEmail("user1@webtech.id");
        user.setPassword("hashed-password1");

        userDetails = new org.springframework.security.core.userdetails.User(
                "user1@webtech.id", "hashed-password1",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("login with valid credentials should return LoginResponse with token")
    void login_withValidCredentials_shouldReturnLoginResponse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("user1@webtech.id")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("mocked-jwt-token");

        LoginResponse response = authService.login(validRequest);

        assertThat(response.getMessage()).isEqualTo("Login success");
        assertThat(response.getUser().getEmail()).isEqualTo("user1@webtech.id");
        assertThat(response.getUser().getName()).isEqualTo("User 1");
        assertThat(response.getUser().getAccessToken()).isEqualTo("mocked-jwt-token");
    }

    @Test
    @DisplayName("login with wrong password should propagate BadCredentialsException")
    void login_withWrongPassword_shouldThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(validRequest))
                .isInstanceOf(BadCredentialsException.class);

        // User lookup should not happen if authentication fails
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login should call authenticationManager with correct email and password")
    void login_shouldAuthenticateWithCorrectCredentials() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.login(validRequest);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user1@webtech.id", "password1")
        );
    }
}
