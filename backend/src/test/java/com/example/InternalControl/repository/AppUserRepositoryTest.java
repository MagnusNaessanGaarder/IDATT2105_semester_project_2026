package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.AppUserLocalCredential;
import com.example.InternalControl.repository.user.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AppUserRepository.
 * Tests custom query methods with @Query annotations.
 */
@SpringBootTest
@Transactional
@DisplayName("AppUserRepository Integration Tests")
class AppUserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        AppUser user = createTestUser("test@example.com", "Test User");
        appUserRepository.save(user);

        // When
        Optional<AppUser> found = appUserRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getDisplayName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<AppUser> found = appUserRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        AppUser user = createTestUser("exists@example.com", "Test User");
        appUserRepository.save(user);

        // When & Then
        assertThat(appUserRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(appUserRepository.existsByEmail("notexists@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find user by email with credentials")
    void shouldFindUserByEmailWithCredentials() {
        // Given
        AppUser user = createTestUserWithCredentials("withcreds@example.com", "Test User", "hashedPassword123");
        appUserRepository.save(user);

        // When
        Optional<AppUser> found = appUserRepository.findByEmailWithCredentials("withcreds@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("withcreds@example.com");
        assertThat(found.get().getLocalCredential()).isNotNull();
        assertThat(found.get().getLocalCredential().getPasswordHash()).isEqualTo("hashedPassword123");
    }

    @Test
    @DisplayName("Should find user by ID with credentials")
    void shouldFindUserByIdWithCredentials() {
        // Given
        AppUser user = createTestUserWithCredentials("byid@example.com", "Test User", "hashedPassword456");
        AppUser saved = appUserRepository.save(user);

        // When
        Optional<AppUser> found = appUserRepository.findByIdWithCredentials(saved.getUserId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(saved.getUserId());
        assertThat(found.get().getLocalCredential()).isNotNull();
    }

    @Test
    @DisplayName("Should find role by email")
    void shouldFindRoleByEmail() {
        // When
        Optional<String> role = appUserRepository.findRoleByEmail("nonexistent@example.com");

        // Then
        assertThat(role).isEmpty();
    }

    private AppUser createTestUser(String email, String displayName) {
        return AppUser.builder()
                .email(email)
                .displayName(displayName)
                .isActive(true)
                .build();
    }

    private AppUser createTestUserWithCredentials(String email, String displayName, String passwordHash) {
        AppUser user = AppUser.builder()
                .email(email)
                .displayName(displayName)
                .isActive(true)
                .build();

        AppUserLocalCredential credential = new AppUserLocalCredential();
        credential.setUser(user);
        credential.setPasswordHash(passwordHash);
        credential.setLastChangedAt(LocalDateTime.now());
        credential.setMustChangePw(false);
        credential.setFailedAttempts(0);

        user.setLocalCredential(credential);
        return user;
    }
}
