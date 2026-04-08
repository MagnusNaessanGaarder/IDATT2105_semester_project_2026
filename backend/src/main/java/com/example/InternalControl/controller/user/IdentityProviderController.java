package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.IdentityResponse;
import com.example.InternalControl.dto.user.LinkIdentityRequest;
import com.example.InternalControl.service.user.IdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for identity provider management.
 * Provides endpoints for managing external identity provider links (SSO).
 */
@RestController
@RequestMapping("/api/v1/identity")
@Tag(name = "Identity Provider", description = "External identity provider integration API")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class IdentityProviderController {

    private final IdentityProviderService identityService;

    @Operation(summary = "Get linked identities for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved identities"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @userSecurity.isCurrentUser(#userId)")
    @Parameter(description = "Identifier of the userId")
    public ResponseEntity<List<IdentityResponse>> getUserIdentities(@PathVariable Long userId) {
        log.info("Getting identities for user: {}", userId);
        return ResponseEntity.ok(identityService.getUserIdentities(userId));
    }

    @Operation(summary = "Link external identity to user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Identity linked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or unsupported provider"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Identity already linked")
    })
    @PostMapping("/user/{userId}/link")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<IdentityResponse> linkIdentity(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @Valid @RequestBody LinkIdentityRequest request) {
        log.info("Linking identity for user: {}, provider: {}", userId, request.getProviderName());
        return ResponseEntity.status(HttpStatus.CREATED).body(identityService.linkIdentity(userId, request));
    }

    @Operation(summary = "Unlink external identity from user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Identity unlinked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Identity not found")
    })
    @DeleteMapping("/user/{userId}/{identityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<Void> unlinkIdentity(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @PathVariable Long identityId) {
        log.info("Unlinking identity: {} for user: {}", identityId, userId);
        identityService.unlinkIdentity(userId, identityId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get supported identity providers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved supported providers"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getSupportedProviders() {
        log.info("Getting supported identity providers");
        return ResponseEntity.ok(identityService.getSupportedProviders());
    }
}
