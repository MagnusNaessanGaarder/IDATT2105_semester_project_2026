package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.IdentityResponse;
import com.example.InternalControl.dto.user.LinkIdentityRequest;
import com.example.InternalControl.model.user.AppUserIdentity;
import com.example.InternalControl.repository.user.AppUserIdentityRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityProviderService.
 */
@ExtendWith(MockitoExtension.class)
class IdentityProviderServiceTest {

    @Mock
    private AppUserIdentityRepository identityRepository;

    @InjectMocks
    private IdentityProviderServiceImpl identityProviderService;

    private static final Long USER_ID = 1L;
    private static final Long IDENTITY_ID = 100L;

    private AppUserIdentity testIdentity;

    @BeforeEach
    void setUp() {
        testIdentity = createTestIdentity(IDENTITY_ID, USER_ID, "google", "google-123");
    }

    // ==================== GET USER IDENTITIES TESTS ====================

    @Test
    void shouldGetUserIdentities() {
        // Given
        AppUserIdentity vippsIdentity = createTestIdentity(101L, USER_ID, "vipps", "vipps-456");
        when(identityRepository.findByUserId(USER_ID))
                .thenReturn(List.of(testIdentity, vippsIdentity));

        // When
        List<IdentityResponse> result = identityProviderService.getUserIdentities(USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProviderName()).isEqualTo("google");
        assertThat(result.get(1).getProviderName()).isEqualTo("vipps");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoIdentities() {
        // Given
        when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        // When
        List<IdentityResponse> result = identityProviderService.getUserIdentities(USER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== LINK IDENTITY TESTS ====================

    @Test
    void shouldLinkIdentity() {
        // Given
        LinkIdentityRequest request = createLinkRequest("google", "google-123", "user@gmail.com");
        when(identityRepository.existsByUserIdAndProviderName(USER_ID, "google")).thenReturn(false);
        when(identityRepository.existsByProviderNameAndProviderUserId("google", "google-123")).thenReturn(false);
        when(identityRepository.save(any(AppUserIdentity.class))).thenAnswer(inv -> {
            AppUserIdentity identity = inv.getArgument(0);
            identity.setIdentityId(IDENTITY_ID);
            return identity;
        });

        // When
        IdentityResponse result = identityProviderService.linkIdentity(USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdentityId()).isEqualTo(IDENTITY_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getProviderName()).isEqualTo("google");
        assertThat(result.getProviderUserId()).isEqualTo("google-123");
        assertThat(result.getProviderEmail()).isEqualTo("user@gmail.com");
    }

    @Test
    void shouldThrowWhenLinkingUnsupportedProvider() {
        // Given
        LinkIdentityRequest request = createLinkRequest("unsupported", "id-123", "user@test.com");

        // When/Then
        assertThatThrownBy(() -> identityProviderService.linkIdentity(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported provider");

        verify(identityRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenIdentityAlreadyLinkedToUser() {
        // Given
        LinkIdentityRequest request = createLinkRequest("google", "google-123", "user@gmail.com");
        when(identityRepository.existsByUserIdAndProviderName(USER_ID, "google")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> identityProviderService.linkIdentity(USER_ID, request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Identity already linked");

        verify(identityRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenProviderUserIdAlreadyLinked() {
        // Given
        LinkIdentityRequest request = createLinkRequest("google", "google-123", "user@gmail.com");
        when(identityRepository.existsByUserIdAndProviderName(USER_ID, "google")).thenReturn(false);
        when(identityRepository.existsByProviderNameAndProviderUserId("google", "google-123")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> identityProviderService.linkIdentity(USER_ID, request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Provider user ID already linked to another account");

        verify(identityRepository, never()).save(any());
    }

    @Test
    void shouldSupportAllSupportedProviders() {
        // Given
        List<String> supportedProviders = identityProviderService.getSupportedProviders();

        // Then
        assertThat(supportedProviders).containsExactlyInAnyOrder("vipps", "google", "microsoft");
    }

    // ==================== UNLINK IDENTITY TESTS ====================

    @Test
    void shouldUnlinkIdentity() {
        // Given
        when(identityRepository.findById(IDENTITY_ID)).thenReturn(java.util.Optional.of(testIdentity));

        // When
        identityProviderService.unlinkIdentity(USER_ID, IDENTITY_ID);

        // Then
        verify(identityRepository).delete(testIdentity);
    }

    @Test
    void shouldThrowWhenUnlinkingNonExistentIdentity() {
        // Given
        when(identityRepository.findById(IDENTITY_ID)).thenReturn(java.util.Optional.empty());

        // When/Then
        assertThatThrownBy(() -> identityProviderService.unlinkIdentity(USER_ID, IDENTITY_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Identity not found");

        verify(identityRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenUnlinkingIdentityNotOwnedByUser() {
        // Given
        AppUserIdentity otherIdentity = createTestIdentity(IDENTITY_ID, 999L, "google", "other-123");
        when(identityRepository.findById(IDENTITY_ID)).thenReturn(java.util.Optional.of(otherIdentity));

        // When/Then
        assertThatThrownBy(() -> identityProviderService.unlinkIdentity(USER_ID, IDENTITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity does not belong to user");

        verify(identityRepository, never()).delete(any());
    }

    // ==================== HELPER METHODS ====================

    private AppUserIdentity createTestIdentity(Long id, Long userId, String provider, String providerUserId) {
        return AppUserIdentity.builder()
                .identityId(id)
                .userId(userId)
                .providerName(provider)
                .providerUserId(providerUserId)
                .providerEmail(userId + "@" + provider + ".com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private LinkIdentityRequest createLinkRequest(String provider, String providerUserId, String email) {
        LinkIdentityRequest request = new LinkIdentityRequest();
        request.setProviderName(provider);
        request.setProviderUserId(providerUserId);
        request.setProviderEmail(email);
        return request;
    }
}
