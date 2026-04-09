package com.example.InternalControl.service.auth;

import com.example.InternalControl.dto.auth.request.LoginRequest;
import com.example.InternalControl.dto.auth.response.OrganizationRoleResponse;
import com.example.InternalControl.dto.auth.request.RegisterRequest;
import com.example.InternalControl.dto.auth.response.AuthResponse;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.AppUserLocalCredential;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationRole;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserOrganizationRepository userOrgRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        LOGGER.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            LOGGER.warn("Email {} already in use", request.getEmail());
            throw new IllegalArgumentException("Email is already in use");
        }

        AppUser user = new AppUser();
        user.setDisplayName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIsActive(true);

        user = userRepository.save(user);
        LOGGER.debug("AppUser created with ID: {}", user.getUserId());

        AppUserLocalCredential credential = new AppUserLocalCredential();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setMustChangePw(false);
        credential.setLastChangedAt(LocalDateTime.now());
        credential.setFailedAttempts(0);

        user.setLocalCredential(credential);
        userRepository.save(user);

        LOGGER.info("User {} registered", request.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Fetch user's organizations and roles
        List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
        String primaryRole = extractPrimaryRole(userDetails);

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), primaryRole, organizations);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        LOGGER.info("Login attempt: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AppUser user = userRepository.findByEmail(request.getEmail()).orElseThrow();
            if (user.getLocalCredential() != null) {
                user.getLocalCredential().resetFailedAttempts();
                LOGGER.debug("Failed attempts reset for {}", request.getEmail());
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Fetch user's organizations and roles
            List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
            String primaryRole = extractPrimaryRole(userDetails);

            LOGGER.info("User {} logged in", request.getEmail());

                return new AuthResponse(accessToken, refreshToken, user.getEmail(), primaryRole, organizations);

        } catch (BadCredentialsException e) {
            AppUser user = userRepository.findByEmailWithCredentials(request.getEmail()).orElse(null);
            if (user != null && user.getLocalCredential() != null) {
                user.getLocalCredential().recordFailedAttempt();
                int attempts = user.getLocalCredential().getFailedAttempts();
                LOGGER.warn("Failed login attempt {} for {}", attempts, request.getEmail());
                
                if (user.isLocked()) {
                    LOGGER.warn("User {} is now locked", request.getEmail());
                    throw new LockedException("Account is temporarily locked after 5 failed attempts. Try again in 30 minutes.");
                }
            }
            
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        LOGGER.info("Token refresh for {}", email);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            LOGGER.warn("Invalid refresh token for {}", email);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getUserId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        LOGGER.info("Token refreshed for {}", email);

        // Fetch user's organizations and roles
        List<OrganizationRoleResponse> organizations = fetchUserOrganizationsAndRoles(user.getUserId());
        String primaryRole = extractPrimaryRole(userDetails);

        return new AuthResponse(newAccessToken, newRefreshToken, email, primaryRole, organizations);
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
                    
                        return new OrganizationRoleResponse(
                            userOrg.getOrganization().getOrgNumber(),
                            userOrg.getOrganization().getDisplayName(),
                            roleName,
                            userOrg.getJoinedAt());
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
