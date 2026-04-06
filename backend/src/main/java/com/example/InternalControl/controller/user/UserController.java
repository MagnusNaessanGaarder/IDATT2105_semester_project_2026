package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.UserCreateRequest;
import com.example.InternalControl.dto.user.UserResponse;
import com.example.InternalControl.dto.user.UserUpdateRequest;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationId;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations.
 * Provides endpoints for CRUD operations on users within an organization.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AppUserRepository userRepository;
    private final UserOrganizationRepository userOrgRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Get all users in an organization.
     *
     * @param orgNumber the organization number
     * @return list of users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam Integer orgNumber) {
        log.info("Getting all users for organization: {}", orgNumber);

        List<UserOrganization> userOrgs = userOrgRepository.findByOrgNumber(orgNumber);

        List<UserResponse> users = userOrgs.stream()
                .map(uo -> mapToUserResponse(uo.getUser(), orgNumber))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * Get a specific user by ID.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @return the user
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId,
            @RequestParam Integer orgNumber) {
        log.info("Getting user: {} for organization: {}", userId, orgNumber);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        return ResponseEntity.ok(mapToUserResponse(user, orgNumber));
    }

    /**
     * Create a new user in an organization.
     *
     * @param request the user creation request
     * @param httpRequest the HTTP request
     * @return the created user
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating user: {} for organization: {}", request.getEmail(), request.getOrgNumber());

        // Check if email already exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create user
        AppUser user = AppUser.builder()
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        // Create organization membership
        UserOrganizationId userOrgId = UserOrganizationId.builder()
                .userId(user.getUserId())
                .orgNumber(request.getOrgNumber())
                .build();

        UserOrganization userOrg = UserOrganization.builder()
                .id(userOrgId)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        userOrgRepository.save(userOrg);

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Long currentUserId = jwtService.extractUserId(
                    extractTokenFromRequest(httpRequest));

            for (Long roleId : request.getRoleIds()) {
                UserOrganizationRoleId roleIdObj = UserOrganizationRoleId.builder()
                        .userId(user.getUserId())
                        .orgNumber(request.getOrgNumber())
                        .roleId(roleId)
                        .build();

                UserOrganizationRole userRole = UserOrganizationRole.builder()
                        .id(roleIdObj)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userOrgRoleRepository.save(userRole);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToUserResponse(user, request.getOrgNumber()));
    }

    /**
     * Update a user.
     *
     * @param userId the user ID
     * @param request the update request
     * @param orgNumber the organization number
     * @return the updated user
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            @RequestParam Integer orgNumber) {
        log.info("Updating user: {} for organization: {}", userId, orgNumber);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Update roles if provided
        if (request.getRoleIds() != null) {
            // Remove existing roles
            List<UserOrganizationRole> existingRoles = userOrgRoleRepository
                    .findByUserIdAndOrgNumber(userId, orgNumber);
            userOrgRoleRepository.deleteAll(existingRoles);

            // Add new roles
            for (Long roleId : request.getRoleIds()) {
                UserOrganizationRoleId roleIdObj = UserOrganizationRoleId.builder()
                        .userId(userId)
                        .orgNumber(orgNumber)
                        .roleId(roleId)
                        .build();

                UserOrganizationRole userRole = UserOrganizationRole.builder()
                        .id(roleIdObj)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userOrgRoleRepository.save(userRole);
            }
        }

        return ResponseEntity.ok(mapToUserResponse(user, orgNumber));
    }

    /**
     * Delete/deactivate a user.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @return no content
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @RequestParam Integer orgNumber) {
        log.info("Deleting user: {} from organization: {}", userId, orgNumber);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Soft delete - deactivate user
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Deactivate organization membership
        UserOrganization userOrg = userOrgRepository
                .findById(new com.example.InternalControl.model.user.UserOrganizationId(userId, orgNumber))
                .orElseThrow(() -> new EntityNotFoundException("User organization not found"));
        userOrg.setIsActive(false);
        userOrg.setLeftAt(LocalDateTime.now());
        userOrgRepository.save(userOrg);

        return ResponseEntity.noContent().build();
    }

    private UserResponse mapToUserResponse(AppUser user, Integer orgNumber) {
        List<UserOrganizationRole> userRoles = userOrgRoleRepository
                .findByUserIdAndOrgNumber(user.getUserId(), orgNumber);

        List<com.example.InternalControl.dto.user.RoleResponse> roles = userRoles.stream()
                .map(uor -> com.example.InternalControl.dto.user.RoleResponse.builder()
                        .roleId(uor.getRoleId())
                        .roleName(uor.getRole() != null ? uor.getRole().getRoleName() : "UNKNOWN")
                        .description(uor.getRole() != null ? uor.getRole().getDescription() : null)
                        .isSystemRole(uor.getRole() != null ? uor.getRole().getIsSystemRole() : false)
                        .build())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .userId(user.getUserId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roles)
                .build();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}