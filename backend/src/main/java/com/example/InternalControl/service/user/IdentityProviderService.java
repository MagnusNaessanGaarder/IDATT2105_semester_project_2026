package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.IdentityResponse;
import com.example.InternalControl.dto.user.LinkIdentityRequest;

import java.util.List;

/**
 * Service interface for identity provider management.
 */
public interface IdentityProviderService {

    /**
     * Get all linked identities for a user.
     *
     * @param userId the user ID
     * @return list of identities
     */
    List<IdentityResponse> getUserIdentities(Long userId);

    /**
     * Link external identity to user.
     *
     * @param userId  the user ID
     * @param request the link request
     * @return the created identity
     */
    IdentityResponse linkIdentity(Long userId, LinkIdentityRequest request);

    /**
     * Unlink external identity from user.
     *
     * @param userId     the user ID
     * @param identityId the identity ID
     */
    void unlinkIdentity(Long userId, Long identityId);

    /**
     * Get list of supported identity providers.
     *
     * @return list of provider names
     */
    List<String> getSupportedProviders();
}
