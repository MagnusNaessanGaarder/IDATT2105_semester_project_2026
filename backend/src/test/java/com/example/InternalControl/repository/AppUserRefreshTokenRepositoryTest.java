package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.TestBlobConfig;
import com.example.InternalControl.model.auth.AppUserRefreshToken;
import com.example.InternalControl.repository.auth.AppUserRefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AppUserRefreshTokenRepository using real database.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBlobConfig.class)
@Transactional
class AppUserRefreshTokenRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private AppUserRefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
  }

  @Test
  void findByTokenHash_ShouldReturnToken_WhenExists() {
    // Given
    AppUserRefreshToken token = createToken("testHash123", 1L, LocalDateTime.now().plusDays(7));
    refreshTokenRepository.save(token);

    // When
    Optional<AppUserRefreshToken> found = refreshTokenRepository.findByTokenHash("testHash123");

    // Then
    assertTrue(found.isPresent());
    assertEquals(1L, found.get().getUserId());
    assertEquals("testHash123", found.get().getTokenHash());
  }

  @Test
  void findByTokenHash_ShouldReturnEmpty_WhenNotExists() {
    // When
    Optional<AppUserRefreshToken> found = refreshTokenRepository.findByTokenHash("nonExistent");

    // Then
    assertFalse(found.isPresent());
  }

  @Test
  void existsByTokenHash_ShouldReturnTrue_WhenExists() {
    // Given
    AppUserRefreshToken token = createToken("existingHash", 1L, LocalDateTime.now().plusDays(7));
    refreshTokenRepository.save(token);

    // When & Then
    assertTrue(refreshTokenRepository.existsByTokenHash("existingHash"));
  }

  @Test
  void existsByTokenHash_ShouldReturnFalse_WhenNotExists() {
    // When & Then
    assertFalse(refreshTokenRepository.existsByTokenHash("nonExistent"));
  }

  @Test
  void deleteByTokenHash_ShouldRemoveToken() {
    // Given
    AppUserRefreshToken token = createToken("toDelete", 1L, LocalDateTime.now().plusDays(7));
    refreshTokenRepository.save(token);
    assertTrue(refreshTokenRepository.existsByTokenHash("toDelete"));

    // When
    refreshTokenRepository.deleteByTokenHash("toDelete");

    // Then
    assertFalse(refreshTokenRepository.existsByTokenHash("toDelete"));
  }

  @Test
  void deleteByUserId_ShouldRemoveAllTokensForUser() {
    // Given
    refreshTokenRepository.save(createToken("hash1", 1L, LocalDateTime.now().plusDays(7)));
    refreshTokenRepository.save(createToken("hash2", 1L, LocalDateTime.now().plusDays(7)));
    refreshTokenRepository.save(createToken("hash3", 2L, LocalDateTime.now().plusDays(7)));
    assertEquals(2, refreshTokenRepository.findAll().stream().filter(t -> t.getUserId().equals(1L)).count());

    // When
    refreshTokenRepository.deleteByUserId(1L);

    // Then
    assertEquals(0, refreshTokenRepository.findAll().stream().filter(t -> t.getUserId().equals(1L)).count());
    assertTrue(refreshTokenRepository.existsByTokenHash("hash3"));
  }

  @Test
  void deleteAllByExpiresAtBefore_ShouldRemoveExpiredTokens() {
    // Given
    LocalDateTime now = LocalDateTime.now();
    refreshTokenRepository.save(createToken("expired1", 1L, now.minusDays(1)));
    refreshTokenRepository.save(createToken("expired2", 2L, now.minusHours(1)));
    refreshTokenRepository.save(createToken("valid", 3L, now.plusDays(7)));
    assertEquals(3, refreshTokenRepository.count());

    // When
    refreshTokenRepository.deleteAllByExpiresAtBefore(now);

    // Then
    assertEquals(1, refreshTokenRepository.count());
    assertTrue(refreshTokenRepository.existsByTokenHash("valid"));
    assertFalse(refreshTokenRepository.existsByTokenHash("expired1"));
    assertFalse(refreshTokenRepository.existsByTokenHash("expired2"));
  }

  @Test
  void save_ShouldStoreToken() {
    // Given
    AppUserRefreshToken token = createToken("newHash", 1L, LocalDateTime.now().plusDays(7));

    // When
    AppUserRefreshToken saved = refreshTokenRepository.save(token);

    // Then
    assertNotNull(saved.getTokenId());
    assertEquals(1L, saved.getUserId());
    assertEquals("newHash", saved.getTokenHash());
    assertTrue(refreshTokenRepository.existsByTokenHash("newHash"));
  }

  private AppUserRefreshToken createToken(String hash, Long userId, LocalDateTime expiresAt) {
    AppUserRefreshToken token = new AppUserRefreshToken();
    token.setTokenHash(hash);
    token.setUserId(userId);
    token.setExpiresAt(expiresAt);
    return token;
  }
}
