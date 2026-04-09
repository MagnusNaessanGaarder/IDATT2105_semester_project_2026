package com.example.InternalControl;

import com.example.InternalControl.model.organization.Organization;
import com.example.InternalControl.model.user.*;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.repository.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Initializes test data for integration tests.
 * Creates users, organizations and roles.
 * 
 * @author TriTacLe
 * @since 1.0
 */
@Component
public class TestDataInitializer {

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private AppUserRepository appUserRepository;

  @Autowired
  private UserOrganizationRepository userOrganizationRepository;

  @Autowired
  private UserOrganizationRoleRepository userOrganizationRoleRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Transactional
  public TestUser createTestUserWithOrg(String email, String fullName, Integer orgNumber) {
    // Create organization if it doesn't exist
    Organization org = organizationRepository.findById(orgNumber)
        .orElseGet(() -> {
          Organization newOrg = new Organization();
          newOrg.setOrgNumber(orgNumber);
          newOrg.setLegalName("Test Organization " + orgNumber);
          newOrg.setDisplayName("Test Org " + orgNumber);
          return organizationRepository.save(newOrg);
        });

    // Get or create MANAGER role (for integration tests that need to create resources)
    Role managerRole = roleRepository.findByRoleName("MANAGER")
        .orElseGet(() -> {
          Role newRole = new Role();
          newRole.setRoleName("MANAGER");
          return roleRepository.save(newRole);
        });

    // Create user
    AppUser user = new AppUser();
    user.setEmail(email);
    user.setDisplayName(fullName);
    user = appUserRepository.save(user);

    // Create local credentials
    AppUserLocalCredential credentials = new AppUserLocalCredential();
    credentials.setUser(user);
    credentials.setPasswordHash(passwordEncoder.encode("TestPass123!"));
    credentials.setLastChangedAt(LocalDateTime.now());
    credentials.setMustChangePw(false);
    user.setLocalCredential(credentials);
    user = appUserRepository.save(user);

    // Associate user with organization
    UserOrganizationId userOrgId = new UserOrganizationId(user.getUserId(), orgNumber);
    UserOrganization userOrg = new UserOrganization();
    userOrg.setId(userOrgId);
    userOrg.setUser(user);
    userOrg.setOrganization(org);
    userOrganizationRepository.save(userOrg);

    // Assign MANAGER role to user in organization
    UserOrganizationRoleId userOrgRoleId = new UserOrganizationRoleId(
        user.getUserId(), orgNumber, managerRole.getRoleId());
    UserOrganizationRole userOrgRole = new UserOrganizationRole();
    userOrgRole.setId(userOrgRoleId);
    userOrgRole.setUser(user);
    userOrgRole.setOrganization(org);
    userOrgRole.setRole(managerRole);
    userOrganizationRoleRepository.save(userOrgRole);

    return new TestUser(user.getUserId(), email, orgNumber);
  }

  public record TestUser(Long userId, String email, Integer orgNumber) {}
}
