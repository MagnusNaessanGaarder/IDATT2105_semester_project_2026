package com.example.InternalControl.service;

import com.example.InternalControl.model.auth.AppUser;
import com.example.InternalControl.model.auth.AppUserRefreshToken;
import com.example.InternalControl.repository.auth.AppUserRefreshTokenRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for refresh token functionality in AuthService.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

  @Mock
  private AppUserRefreshTokenRepository refreshTokenRepository;

  @Mock
  private AppUserRepository userRepository;

  @Mock
  private JwtService jwtService;

    @InjectMocks
    private com.example.InternalControl.service.auth.AuthService authService;

  private static final String TEST_EMAIL = "test@example.com";
  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_TOKEN = "validRefreshToken";

  @Test
  void logout_ShouldRevokeToken() {
    // Given
    AppUserRefreshToken token = new AppUserRefreshToken();
    token.setTokenHash("hashedToken");
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

    // When
    authService.logout(TEST_TOKEN);

    // Then
    assertNotNull(token.getRevokedAt());
    verify(refreshTokenRepository).save(token);
  }

  @Test
  void logoutAllDevices_ShouldDeleteAllTokensForUser() {
    // When
    authService.logoutAllDevices(TEST_USER_ID);

    // Then
    verify(refreshTokenRepository).deleteByUserId(TEST_USER_ID);
  }

  @Test
  void refreshToken_ShouldThrowException_WhenTokenNotInDatabase() {
    // Given
    when(jwtService.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(TEST_TOKEN));
  }

  @Test
  void refreshToken_ShouldThrowException_WhenTokenExpired() {
    // Given
    AppUserRefreshToken expiredToken = new AppUserRefreshToken();
    expiredToken.setTokenHash("hashedToken");
    expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1));

    when(jwtService.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(TEST_TOKEN));
    verify(refreshTokenRepository).delete(expiredToken);
  }

  @Test
  void refreshToken_ShouldThrowException_WhenTokenRevoked() {
    // Given
    AppUserRefreshToken revokedToken = new AppUserRefreshToken();
    revokedToken.setTokenHash("hashedToken");
    revokedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    revokedToken.setRevokedAt(LocalDateTime.now());

    when(jwtService.extractUsername(TEST_TOKEN)).thenReturn(TEST_EMAIL);
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revokedToken));

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(TEST_TOKEN));
  }

  @Test
  void hashToken_ShouldGenerateConsistentHash() {
    // Given
    String token = "mySecretToken";

    // When
    String hash1 = authService.hashToken(token);
    String hash2 = authService.hashToken(token);

    // Then
    assertEquals(hash1, hash2);
    assertNotNull(hash1);
    assertNotEquals(token, hash1);
  }
}
