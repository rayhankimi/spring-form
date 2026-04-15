package com.rayhank.tech_assesment.controller;

import com.rayhank.tech_assesment.dto.auth.AuthResponse;
import com.rayhank.tech_assesment.dto.auth.LoginRequest;
import com.rayhank.tech_assesment.dto.auth.RegisterRequest;
import com.rayhank.tech_assesment.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoint untuk register dan login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements   // endpoint ini tidak butuh JWT
    @Operation(summary = "Daftar akun baru")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements   // endpoint ini tidak butuh JWT
    @Operation(summary = "Login dan dapatkan token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
