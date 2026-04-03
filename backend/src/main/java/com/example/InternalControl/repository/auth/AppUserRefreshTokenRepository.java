package com.example.InternalControl.repository.auth;

import com.example.InternalControl.model.auth.AppUserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for refresh token management.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface AppUserRefreshTokenRepository extends JpaRepository<AppUserRefreshToken, Long> {

  Optional<AppUserRefreshToken> findByTokenHash(String tokenHash);

  boolean existsByTokenHash(String tokenHash);

  void deleteByTokenHash(String tokenHash);

  void deleteByUserId(Long userId);

  void deleteAllByExpiresAtBefore(LocalDateTime dateTime);
}
