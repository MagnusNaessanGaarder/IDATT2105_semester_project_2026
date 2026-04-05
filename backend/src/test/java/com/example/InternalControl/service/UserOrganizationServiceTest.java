package com.example.InternalControl.service;

import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.service.user.UserOrganizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserOrganizationService.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserOrganizationServiceTest {

    @Mock
    private UserOrganizationRepository userOrgRepository;

    @InjectMocks
    private UserOrganizationService userOrgService;

    @Test
    @DisplayName("Should return true when user is in organization")
    void shouldReturnTrueWhenUserIsInOrganization() {
        // Given
        Long userId = 1L;
        Integer orgNumber = 937219997;
        when(userOrgRepository.existsByUserIdAndOrgNumber(userId, orgNumber)).thenReturn(true);

        // When
        boolean result = userOrgService.isUserInOrganization(userId, orgNumber);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is not in organization")
    void shouldReturnFalseWhenUserIsNotInOrganization() {
        // Given
        Long userId = 1L;
        Integer orgNumber = 999999999;
        when(userOrgRepository.existsByUserIdAndOrgNumber(userId, orgNumber)).thenReturn(false);

        // When
        boolean result = userOrgService.isUserInOrganization(userId, orgNumber);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user ID is null")
    void shouldReturnFalseWhenUserIdIsNull() {
        // Given
        Integer orgNumber = 937219997;
        when(userOrgRepository.existsByUserIdAndOrgNumber(null, orgNumber)).thenReturn(false);

        // When
        boolean result = userOrgService.isUserInOrganization(null, orgNumber);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when org number is null")
    void shouldReturnFalseWhenOrgNumberIsNull() {
        // Given
        Long userId = 1L;
        when(userOrgRepository.existsByUserIdAndOrgNumber(userId, null)).thenReturn(false);

        // When
        boolean result = userOrgService.isUserInOrganization(userId, null);

        // Then
        assertThat(result).isFalse();
    }
}
