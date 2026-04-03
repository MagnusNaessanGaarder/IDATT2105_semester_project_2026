package com.example.InternalControl.service.auth;

import com.example.InternalControl.dto.auth.AuthResponse;
import com.example.InternalControl.dto.auth.LoginRequest;
import com.example.InternalControl.dto.user.OrganizationRoleResponse;
import com.example.InternalControl.dto.auth.RegisterRequest;
import com.example.InternalControl.model.auth.AppUser;
import com.example.InternalControl.model.auth.AppUserLocalCredential;
import com.example.InternalControl.model.auth.AppUserRefreshToken;
import com.example.InternalControl.model.organization.UserOrganization;
import com.example.InternalControl.model.organization.UserOrganizationRole;
import com.example.InternalControl.repository.auth.AppUserRefreshTokenRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Service for authentication.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository userRepository;
    private final AppUserRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserOrganizationRepository userOrgRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        LOGGER.info("Registering new user: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            LOGGER.warn("Email {} already in use", request.email());
            throw new IllegalArgumentException("Email is already in use");
        }

        AppUser user = AppUser.builder()
                .displayName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        LOGGER.debug("AppUser created with ID: {}", user.getUserId());

        AppUserLocalCredential credential = AppUserLocalCredential.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(request.password()))
                .mustChangePw(false)
                .lastChangedAt(LocalDateTime.now())
                .failedAttempts(0)
                .build();

        user.setLocalCredential(credential);
        userRepository.save(user);

        LOGGER.info("User {} registered", request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        storeRefreshToken(user.getUserId(), refreshToken);
        List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
        String primaryRole = extractPrimaryRole(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(primaryRole)
                .organizations(organizations)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        LOGGER.info("Login attempt: {}", request.email());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            AppUser user = userRepository.findByEmail(request.email()).orElseThrow();
            if (user.getLocalCredential() != null) {
                user.getLocalCredential().resetFailedAttempts();
                LOGGER.debug("Failed attempts reset for {}", request.email());
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            storeRefreshToken(user.getUserId(), refreshToken);
            List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
            String primaryRole = extractPrimaryRole(userDetails);

            LOGGER.info("User {} logged in", request.email());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .role(primaryRole)
                    .organizations(organizations)
                    .build();

        } catch (BadCredentialsException e) {
            AppUser user = userRepository.findByEmailWithCredentials(request.email()).orElse(null);
            if (user != null && user.getLocalCredential() != null) {
                user.getLocalCredential().recordFailedAttempt();
                int attempts = user.getLocalCredential().getFailedAttempts();
                LOGGER.warn("Failed login attempt {} for {}", attempts, request.email());
                
                if (user.isLocked()) {
                    LOGGER.warn("User {} is now locked", request.email());
                    throw new LockedException("Account is temporarily locked after 5 failed attempts. Try again in 30 minutes.");
                }
            }
            
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        LOGGER.info("Token refresh for {}", email);

        String tokenHash = hashToken(refreshToken);
        AppUserRefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    LOGGER.warn("Refresh token not found in database for {}", email);
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            LOGGER.warn("Refresh token expired for {}", email);
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        if (storedToken.getRevokedAt() != null) {
            LOGGER.warn("Refresh token revoked for {}", email);
            throw new IllegalArgumentException("Refresh token revoked");
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            LOGGER.warn("Invalid refresh token signature for {}", email);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        refreshTokenRepository.delete(storedToken);
        
        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        storeRefreshToken(user.getUserId(), newRefreshToken);

        LOGGER.info("Token refreshed for {}", email);

        List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
        String primaryRole = extractPrimaryRole(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(email)
                .role(primaryRole)
                .organizations(organizations)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
            LOGGER.info("Refresh token revoked for user {}", token.getUserId());
        });
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        LOGGER.info("All refresh tokens revoked for user {}", userId);
    }

    /**
     * Store refresh token in database with SHA-256 hash.
     */
    private void storeRefreshToken(Long userId, String rawToken) {
        String tokenHash = hashToken(rawToken);
        
        LocalDateTime expiresAt = jwtService.extractExpiration(rawToken);
        
        AppUserRefreshToken token = new AppUserRefreshToken();
        token.setUserId(userId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        
        refreshTokenRepository.save(token);
        LOGGER.debug("Refresh token stored for user {}", userId);
    }

    /**
     * Hash token using SHA-256 for secure storage.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public String getRoleByEmail(String email) {
        return userRepository.findRoleByEmail(email).orElse("EMPLOYEE");
    }

    /**
     * Fetch all organizations and roles for a user.
     */
    @Transactional(readOnly = true)
    public List<OrganizationRoleResponse> fetchUserOrganizationsAndRoles(Long userId) {
        List<UserOrganization> userOrgs = userOrgRepository.findActiveOrganizationsByUserId(userId);
        
        return userOrgs.stream()
                .map(userOrg -> {
                    List<UserOrganizationRole> roles = userOrgRoleRepository.findByUserOrganization(
                            userOrg.getUser().getUserId(),
                            userOrg.getOrganization().getOrgNumber()
                    );
                    
                    String roleName = roles.isEmpty() ? "EMPLOYEE" 
                            : roles.get(0).getRole().getRoleName();
                    
                    return OrganizationRoleResponse.builder()
                            .orgNumber(userOrg.getOrganization().getOrgNumber())
                            .orgName(userOrg.getOrganization().getDisplayName())
                            .role(roleName)
                            .joinedAt(userOrg.getJoinedAt())
                            .build();
                })
                .toList();
    }

    /**
     * Extract primary role from UserDetails authorities.
     */
    private String extractPrimaryRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("EMPLOYEE");
    }
}
