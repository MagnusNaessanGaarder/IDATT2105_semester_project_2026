package com.example.InternalControl.service.auth;

import com.example.InternalControl.model.organization.Organization;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.AppUserLocalCredential;
import com.example.InternalControl.model.user.Role;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationId;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private UserOrganizationRepository userOrgRepository;

    @Mock
    private UserOrganizationRoleRepository userOrgRoleRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;
    private static final String PASSWORD_HASH = "hashedPassword123";

    private AppUser testUser;
    private AppUserLocalCredential testCredential;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(USER_ID, TEST_EMAIL, true);
        testCredential = createTestCredential(PASSWORD_HASH);
        testUser.setLocalCredential(testCredential);
    }

    // ==================== SUCCESSFUL LOADING TESTS ====================

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Given
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(Collections.emptyList());

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
        assertThat(result.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void shouldLoadUserWithCustomUserDetailsType() {
        // Given
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(Collections.emptyList());

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails customDetails = (CustomUserDetails) result;
        assertThat(customDetails.getUserId()).isEqualTo(USER_ID);
    }

    // ==================== ROLE ASSIGNMENT TESTS ====================

    @Test
    void shouldLoadUserWithMultipleRoles() {
        // Given
        Organization org = createTestOrganization(123);
        UserOrganization userOrg = createTestUserOrganization(testUser, org);
        UserOrganizationRole adminRole = createTestUserOrganizationRole(testUser, org, 1L, "ADMIN");
        UserOrganizationRole managerRole = createTestUserOrganizationRole(testUser, org, 2L, "MANAGER");

        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(List.of(userOrg));
        when(userOrgRoleRepository.findByUserOrganization(USER_ID, 123))
                .thenReturn(List.of(adminRole, managerRole));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result.getAuthorities()).hasSize(2);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    void shouldAssignDefaultEmployeeRoleWhenNoRolesFound() {
        // Given
        Organization org = createTestOrganization(123);
        UserOrganization userOrg = createTestUserOrganization(testUser, org);

        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(List.of(userOrg));
        when(userOrgRoleRepository.findByUserOrganization(USER_ID, 123))
                .thenReturn(Collections.emptyList());

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_EMPLOYEE");
    }

    // ==================== ERROR SCENARIO TESTS ====================

    @Test
    void shouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrowWhenUserHasNoLocalCredential() {
        // Given
        testUser.setLocalCredential(null);
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrowWhenUserIsLocked() {
        // Given
        testCredential.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Account is temporarily locked");
    }

    @Test
    void shouldThrowWhenUserIsInactive() {
        // Given
        testUser.setIsActive(false);
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Account is disabled");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void shouldLoadUserWhenLockExpired() {
        // Given
        testCredential.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(Collections.emptyList());

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAccountNonLocked()).isTrue();
    }

    @Test
    void shouldLoadUserWithMultipleOrganizations() {
        // Given
        Organization org1 = createTestOrganization(123);
        Organization org2 = createTestOrganization(456);
        UserOrganization userOrg1 = createTestUserOrganization(testUser, org1);
        UserOrganization userOrg2 = createTestUserOrganization(testUser, org2);
        UserOrganizationRole role1 = createTestUserOrganizationRole(testUser, org1, 1L, "ADMIN");
        UserOrganizationRole role2 = createTestUserOrganizationRole(testUser, org2, 2L, "USER");

        when(userRepository.findByEmailWithCredentials(TEST_EMAIL))
                .thenReturn(Optional.of(testUser));
        when(userOrgRepository.findActiveOrganizationsByUserId(USER_ID))
                .thenReturn(List.of(userOrg1, userOrg2));
        when(userOrgRoleRepository.findByUserOrganization(USER_ID, 123))
                .thenReturn(List.of(role1));
        when(userOrgRoleRepository.findByUserOrganization(USER_ID, 456))
                .thenReturn(List.of(role2));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

        // Then
        assertThat(result.getAuthorities()).hasSize(2);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    // ==================== HELPER METHODS ====================

    private AppUser createTestUser(Long userId, String email, boolean active) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setDisplayName("Test User");
        user.setIsActive(active);
        return user;
    }

    private AppUserLocalCredential createTestCredential(String passwordHash) {
        AppUserLocalCredential credential = new AppUserLocalCredential();
        credential.setCredentialId(1L);
        credential.setPasswordHash(passwordHash);
        credential.setFailedAttempts(0);
        return credential;
    }

    private Organization createTestOrganization(Integer orgNumber) {
        Organization org = new Organization();
        org.setOrgNumber(orgNumber);
        org.setLegalName("Test Org " + orgNumber);
        return org;
    }

    private UserOrganization createTestUserOrganization(AppUser user, Organization org) {
        UserOrganizationId id = new UserOrganizationId(user.getUserId(), org.getOrgNumber());
        UserOrganization userOrg = new UserOrganization();
        userOrg.setId(id);
        userOrg.setUser(user);
        userOrg.setOrganization(org);
        userOrg.setIsActive(true);
        return userOrg;
    }

    private UserOrganizationRole createTestUserOrganizationRole(AppUser user, Organization org, Long roleId, String roleName) {
        UserOrganizationRoleId id = new UserOrganizationRoleId(user.getUserId(), org.getOrgNumber(), roleId);
        UserOrganizationRole userOrgRole = new UserOrganizationRole();
        userOrgRole.setId(id);
        userOrgRole.setUser(user);
        userOrgRole.setOrganization(org);

        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        userOrgRole.setRole(role);

        return userOrgRole;
    }
}
