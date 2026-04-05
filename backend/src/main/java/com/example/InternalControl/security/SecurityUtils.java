package com.example.InternalControl.security;

import com.example.InternalControl.service.user.UserOrganizationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

  private static final int BEARER_PREFIX_LENGTH = 7;
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserOrganizationService userOrgService;

  public Long extractUserId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(BEARER_PREFIX_LENGTH);
    Long userId = jwtService.extractUserId(token);
    if (userId == null) {
      throw new IllegalArgumentException("User ID not found in token");
    }
    return userId;
  }

  public void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }

  public Long extractAndValidateUser(HttpServletRequest request, Integer orgNumber) {
    Long userId = extractUserId(request);
    validateUserOrganizationAccess(userId, orgNumber);
    return userId;
  }
}
