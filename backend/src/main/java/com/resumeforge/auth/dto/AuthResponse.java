package com.resumeforge.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private int expiresIn;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private UUID id;
        private String name;
        private String email;
    }
}
