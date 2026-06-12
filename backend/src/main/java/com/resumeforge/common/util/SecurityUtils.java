package com.resumeforge.common.util;

import com.resumeforge.auth.security.UserPrincipal;
import com.resumeforge.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for extracting the current authenticated user from Spring Security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Extracts the current user's UUID from the Spring Security context.
     * Throws UnauthorizedException if no authenticated user is found.
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        throw new UnauthorizedException("Unable to extract user identity");
    }
}