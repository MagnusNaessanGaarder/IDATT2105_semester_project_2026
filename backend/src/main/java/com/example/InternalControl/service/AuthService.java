package com.example.InternalControl.service;

import com.example.InternalControl.dto.AuthResponse;
import com.example.InternalControl.dto.LoginRequest;
import com.example.InternalControl.dto.RegisterRequest;
import com.example.InternalControl.model.AppUser;
import com.example.InternalControl.model.AppUserLocalCredential;
import com.example.InternalControl.repository.AppUserRepository;
import com.example.InternalControl.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationProvider authenticationProvider;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registering new user: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            logger.warn("Email {} already in use", request.email());
            throw new IllegalArgumentException("Email is already in use");
        }

        AppUser user = AppUser.builder()
                .displayName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        logger.debug("AppUser created with ID: {}", user.getUserId());

        AppUserLocalCredential credential = AppUserLocalCredential.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(request.password()))
                .mustChangePw(false)
                .lastChangedAt(LocalDateTime.now())
                .failedAttempts(0)
                .build();

        user.setLocalCredential(credential);
        userRepository.save(user);

        logger.info("User {} registered", request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String role = getRoleByEmail(user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), role);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt: {}", request.email());

        try {
            Authentication authentication = authenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            AppUser user = userRepository.findByEmail(request.email()).orElseThrow();
            if (user.getLocalCredential() != null) {
                user.getLocalCredential().resetFailedAttempts();
                logger.debug("Failed attempts reset for {}", request.email());
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            String role = getRoleByEmail(user.getEmail());
            logger.info("User {} logged in", request.email());

            return new AuthResponse(accessToken, refreshToken, user.getEmail(), role);

        } catch (BadCredentialsException e) {
            AppUser user = userRepository.findByEmailWithCredentials(request.email()).orElse(null);
            if (user != null && user.getLocalCredential() != null) {
                user.getLocalCredential().recordFailedAttempt();
                int attempts = user.getLocalCredential().getFailedAttempts();
                logger.warn("Failed login attempt {} for {}", attempts, request.email());
                
                if (user.isLocked()) {
                    logger.warn("User {} is now locked", request.email());
                    throw new LockedException("Account is temporarily locked after 5 failed attempts. Try again in 30 minutes.");
                }
            }
            
            throw new BadCredentialsException("Invalid email or password");
        } catch (Exception e) {
            logger.error("Unexpected error during login for {}: {}", request.email(), e.getClass().getName());
            logger.error("Exception message: {}", e.getMessage());
            throw e;
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        logger.info("Token refresh for {}", email);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            logger.warn("Invalid refresh token for {}", email);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        logger.info("Token refreshed for {}", email);

        String role = getRoleByEmail(email);
        return new AuthResponse(newAccessToken, newRefreshToken, email, role);
    }

    @Transactional(readOnly = true)
    public String getRoleByEmail(String email) {
        return userRepository.findRoleByEmail(email).orElse("EMPLOYEE");
    }
}
