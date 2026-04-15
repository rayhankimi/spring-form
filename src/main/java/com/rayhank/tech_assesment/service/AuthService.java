package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.auth.LoginRequest;
import com.rayhank.tech_assesment.dto.auth.LoginResponse;
import com.rayhank.tech_assesment.dto.auth.UserTokenDto;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.UserRepository;
import com.rayhank.tech_assesment.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException if email/password is wrong — caught by GlobalExceptionHandler
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return LoginResponse.builder()
                .message("Login success")
                .user(UserTokenDto.builder()
                        .name(user.getName())
                        .email(user.getEmail())
                        .accessToken(token)
                        .build())
                .build();
    }
}
