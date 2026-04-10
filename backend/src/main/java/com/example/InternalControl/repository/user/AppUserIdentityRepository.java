package com.example.InternalControl.repository.user;

import com.example.InternalControl.model.user.AppUserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AppUserIdentity entity.
 */
@Repository
public interface AppUserIdentityRepository extends JpaRepository<AppUserIdentity, Long> {

    List<AppUserIdentity> findByUserId(Long userId);

    Optional<AppUserIdentity> findByUserIdAndProviderName(Long userId, String providerName);

    boolean existsByUserIdAndProviderName(Long userId, String providerName);

    boolean existsByProviderNameAndProviderUserId(String providerName, String providerUserId);

    long deleteByUserId(Long userId);
}
