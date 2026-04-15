package com.rayhank.tech_assesment.controller;

import com.rayhank.tech_assesment.dto.MessageResponse;
import com.rayhank.tech_assesment.dto.auth.LoginRequest;
import com.rayhank.tech_assesment.dto.auth.LoginResponse;
import com.rayhank.tech_assesment.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login and receive a JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current session (client must discard the token)")
    public ResponseEntity<MessageResponse> logout() {
        // JWT is stateless — no server-side token storage to clear.
        // The filter chain has already validated the token before this method is reached.
        // The client is responsible for discarding the token after this call.
        return ResponseEntity.ok(new MessageResponse("Logout success"));
    }
}
