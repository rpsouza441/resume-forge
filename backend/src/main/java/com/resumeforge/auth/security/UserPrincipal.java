package com.resumeforge.auth.security;

import java.util.UUID;

/**
 * Custom principal that holds the userId extracted from the JWT.
 * This allows downstream code to access the userId without
 * re-parsing the token.
 */
public class UserPrincipal {

    private final UUID userId;
    private final String email;

    public UserPrincipal(UUID userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return email;
    }
}
