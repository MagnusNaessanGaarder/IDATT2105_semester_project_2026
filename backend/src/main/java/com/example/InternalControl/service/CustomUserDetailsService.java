package com.example.InternalControl.service;

import com.example.InternalControl.model.AppUser;
import com.example.InternalControl.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final AppUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user: {}", email);
        
        AppUser user = userRepository.findByEmailWithCredentials(email)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", email);
                    return new UsernameNotFoundException("Invalid email or password");
                });

        if (user.getLocalCredential() == null) {
            logger.warn("User {} has no password-based login", email);
            throw new UsernameNotFoundException("Invalid email or password");
        }

        if (user.isLocked()) {
            logger.warn("User {} is locked until {}", email, user.getLocalCredential().getLockedUntil());
            throw new UsernameNotFoundException("Account is temporarily locked. Try again later.");
        }

        if (!user.isActive()) {
            logger.warn("User {} is disabled", email);
            throw new UsernameNotFoundException("Account is disabled");
        }

        // Simplified, gets default role
        // TODO: Extend to get actual role from user_organization_role table
        String role = "EMPLOYEE";

        logger.info("User {} authenticated with role {}", email, role);

        return User.builder()
                .username(user.getEmail())
                .password(user.getLocalCredential().getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .accountLocked(user.isLocked())
                .disabled(!user.isActive())
                .build();
    }
}
