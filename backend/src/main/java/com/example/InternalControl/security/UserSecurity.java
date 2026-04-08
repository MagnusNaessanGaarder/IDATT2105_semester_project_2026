package com.example.InternalControl.security;

import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security utility class for user-related authorization checks.
 * Used in @PreAuthorize annotations.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final AppUserRepository userRepository;

    /**
     * Check if the currently authenticated user has the given user ID.
     *
     * @param userId the user ID to check
     * @return true if current user matches
     */
    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String email = authentication.getName();
        if (email == null) {
            return false;
        }

        return userRepository.findByEmail(email)
                .map(user -> user.getUserId().equals(userId))
                .orElse(false);
    }
}