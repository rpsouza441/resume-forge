package com.resumeforge.auth.service;

import com.resumeforge.auth.dto.AuthResponse;
import com.resumeforge.auth.dto.LoginRequest;
import com.resumeforge.auth.dto.RegisterRequest;
import com.resumeforge.auth.entity.User;
import com.resumeforge.auth.repository.UserRepository;
import com.resumeforge.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.resumeforge.exception.ValidationException(
                    "Email ja cadastrado.",
                    java.util.Map.of("email", "Este email ja esta em uso.")
            );
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        int expiresIn = (int) (jwtTokenProvider.getExpirationMs() / 1000);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.resumeforge.exception.UnauthorizedException(
                        "Credenciais invalidas."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new com.resumeforge.exception.UnauthorizedException(
                    "Credenciais invalidas.");
        }

        if (!user.getIsActive()) {
            throw new com.resumeforge.exception.UnauthorizedException(
                    "Conta desativada.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        int expiresIn = (int) (jwtTokenProvider.getExpirationMs() / 1000);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.resumeforge.exception.ResourceNotFoundException(
                        "Usuario nao encontrado."));
    }
}
