package com.resumeforge.auth.controller;

import com.resumeforge.auth.dto.AuthResponse;
import com.resumeforge.auth.dto.LoginRequest;
import com.resumeforge.auth.dto.RegisterRequest;
import com.resumeforge.auth.entity.User;
import com.resumeforge.auth.security.JwtAuthenticationFilter;
import com.resumeforge.auth.security.JwtTokenProvider;
import com.resumeforge.auth.security.UserPrincipal;
import com.resumeforge.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT — logout is handled client-side by discarding the token.
        // No server-side token blacklist in the MVP.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "unauthorized",
                            "message", "Nao autenticado.",
                            "timestamp", java.time.OffsetDateTime.now().toString()
                    ));
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        User user = authService.getCurrentUser(userPrincipal.getUserId());

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt().toString(),
                "lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null
        ));
    }
}
