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
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.repository.user.RoleRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations. Provides endpoints for CRUD
 * operations on users within an organization.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Manage users within organizations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AppUserRepository userRepository;
    private final UserOrganizationRepository userOrgRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
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
    @Transactional(readOnly = true)
    @Operation(summary = "Get all users", description = "Retrieve all users in an organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @Parameter(description = "The orgNumber parameter")
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
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "Identifier of the userId")
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
    @Operation(summary = "Create user", description = "Create a new user in an organization (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
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
        com.example.InternalControl.model.organization.Organization organization =
                organizationRepository.findById(request.getOrgNumber())
                        .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrgNumber()));

        UserOrganizationId userOrgId = UserOrganizationId.builder()
                .userId(user.getUserId())
                .orgNumber(request.getOrgNumber())
                .build();

        UserOrganization userOrg = UserOrganization.builder()
                .id(userOrgId)
                .user(user)
                .organization(organization)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        userOrgRepository.save(userOrg);

        // Assign roles if provided — must set user, organization AND role for @MapsId
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                com.example.InternalControl.model.user.Role roleEntity = roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

                UserOrganizationRoleId roleIdObj = UserOrganizationRoleId.builder()
                        .userId(user.getUserId())
                        .orgNumber(request.getOrgNumber())
                        .roleId(roleId)
                        .build();

                UserOrganizationRole userRole = UserOrganizationRole.builder()
                        .id(roleIdObj)
                        .user(user)
                        .organization(organization)
                        .role(roleEntity)
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
    @Operation(summary = "Update user", description = "Update user details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully updated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Identifier of the userId")
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

            com.example.InternalControl.model.organization.Organization organization =
                    organizationRepository.findById(orgNumber)
                            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgNumber));

            // Add new roles — must set user, organization AND role for @MapsId
            for (Long roleId : request.getRoleIds()) {
                com.example.InternalControl.model.user.Role roleEntity = roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

                UserOrganizationRoleId roleIdObj = UserOrganizationRoleId.builder()
                        .userId(userId)
                        .orgNumber(orgNumber)
                        .roleId(roleId)
                        .build();

                UserOrganizationRole userRole = UserOrganizationRole.builder()
                        .id(roleIdObj)
                        .user(user)
                        .organization(organization)
                        .role(roleEntity)
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
    @Operation(summary = "Delete user", description = "Soft delete (deactivate) a user (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User successfully deactivated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Identifier of the userId")
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

    /**
     * Add an existing user to an organization by email.
     * The user must have already registered an account.
     *
     * @param request the add-to-org request
     * @return the user response, or 404 if no account exists for that email
     */
    @PostMapping("/add-to-org")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Add existing user to org", description = "Add a pre-registered user to an organization by email (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User added to organization"),
            @ApiResponse(responseCode = "404", description = "No account found for that email"),
            @ApiResponse(responseCode = "409", description = "User is already a member of this organization"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<UserResponse> addToOrg(
            @RequestBody AddToOrgRequest request) {

        log.info("Adding user by email {} to organization {}", request.getEmail(), request.getOrgNumber());

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No account found for email: " + request.getEmail()));

        // Check not already a member
        if (userOrgRepository.existsByUserIdAndOrgNumber(user.getUserId(), request.getOrgNumber())) {
            throw new IllegalStateException("User is already a member of this organization");
        }

        com.example.InternalControl.model.organization.Organization organization =
                organizationRepository.findById(request.getOrgNumber())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Organization not found: " + request.getOrgNumber()));

        // Must set both entity references so @MapsId can derive the composite key
        UserOrganizationId userOrgId = UserOrganizationId.builder()
                .userId(user.getUserId())
                .orgNumber(request.getOrgNumber())
                .build();

        UserOrganization userOrg = UserOrganization.builder()
                .id(userOrgId)
                .user(user)
                .organization(organization)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        userOrgRepository.save(userOrg);

        // Assign roles — must set user, organization AND role for @MapsId to resolve
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                com.example.InternalControl.model.user.Role roleEntity = roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

                UserOrganizationRoleId roleIdObj = UserOrganizationRoleId.builder()
                        .userId(user.getUserId())
                        .orgNumber(request.getOrgNumber())
                        .roleId(roleId)
                        .build();

                UserOrganizationRole userRole = UserOrganizationRole.builder()
                        .id(roleIdObj)
                        .user(user)
                        .organization(organization)
                        .role(roleEntity)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userOrgRoleRepository.save(userRole);
            }
        }

        return ResponseEntity.ok(mapToUserResponse(user, request.getOrgNumber()));
    }

    /** Simple inner DTO for the add-to-org request. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AddToOrgRequest {
        private String email;
        private Integer orgNumber;
        private java.util.List<Long> roleIds;
    }

    private UserResponse mapToUserResponse(AppUser user, Integer orgNumber) {
        List<UserOrganizationRole> userRoles = userOrgRoleRepository
                .findByUserIdAndOrgNumber(user.getUserId(), orgNumber);

        List<com.example.InternalControl.dto.user.RoleResponse> roles = userRoles.stream()
                .map(uor -> com.example.InternalControl.dto.user.RoleResponse.builder()
                        .roleId(uor.getId().getRoleId())
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