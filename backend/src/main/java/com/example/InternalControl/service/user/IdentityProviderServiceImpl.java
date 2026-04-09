package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.IdentityResponse;
import com.example.InternalControl.dto.user.LinkIdentityRequest;
import com.example.InternalControl.model.user.AppUserIdentity;
import com.example.InternalControl.repository.user.AppUserIdentityRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of IdentityProviderService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class IdentityProviderServiceImpl implements IdentityProviderService {

    private final AppUserIdentityRepository identityRepository;

    private static final List<String> SUPPORTED_PROVIDERS = Arrays.asList("vipps", "google", "microsoft");

    @Override
    public List<IdentityResponse> getUserIdentities(Long userId) {
        log.debug("Getting identities for user: {}", userId);
        return identityRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IdentityResponse linkIdentity(Long userId, LinkIdentityRequest request) {
        log.info("Linking identity for user: {}, provider: {}", userId, request.getProviderName());

        // Validate provider
        if (!SUPPORTED_PROVIDERS.contains(request.getProviderName().toLowerCase())) {
            throw new IllegalArgumentException("Unsupported provider: " + request.getProviderName());
        }

        // Check if identity already linked to this user
        if (identityRepository.existsByUserIdAndProviderName(userId, request.getProviderName())) {
            throw new EntityExistsException("Identity already linked for user " + userId + " with provider " + request.getProviderName());
        }

        // Check if provider user ID already used by another user
        if (identityRepository.existsByProviderNameAndProviderUserId(request.getProviderName(), request.getProviderUserId())) {
            throw new EntityExistsException("Provider user ID already linked to another account");
        }

        AppUserIdentity identity = AppUserIdentity.builder()
                .userId(userId)
                .providerName(request.getProviderName())
                .providerUserId(request.getProviderUserId())
                .providerEmail(request.getProviderEmail())
                .createdAt(LocalDateTime.now())
                .build();

        identity = identityRepository.save(identity);
        log.info("Identity linked: {} for user {}", identity.getIdentityId(), userId);

        return mapToResponse(identity);
    }

    @Override
    @Transactional
    public void unlinkIdentity(Long userId, Long identityId) {
        log.info("Unlinking identity: {} for user: {}", identityId, userId);

        AppUserIdentity identity = identityRepository.findById(identityId)
                .orElseThrow(() -> new EntityNotFoundException("Identity not found: " + identityId));

        if (!identity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Identity does not belong to user " + userId);
        }

        identityRepository.delete(identity);
        log.info("Identity unlinked: {}", identityId);
    }

    @Override
    public List<String> getSupportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    private IdentityResponse mapToResponse(AppUserIdentity identity) {
        return IdentityResponse.builder()
                .identityId(identity.getIdentityId())
                .userId(identity.getUserId())
                .providerName(identity.getProviderName())
                .providerUserId(identity.getProviderUserId())
                .providerEmail(identity.getProviderEmail())
                .createdAt(identity.getCreatedAt())
                .build();
    }
}
