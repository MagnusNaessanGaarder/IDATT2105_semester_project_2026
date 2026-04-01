package com.example.InternalControl.service;

import com.example.InternalControl.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-organization membership operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserOrganizationService {

    private final UserOrganizationRepository userOrgRepository;

    /**
     * Check if a user is a member of an organization.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @return true if user is a member
     */
    @Transactional(readOnly = true)
    public boolean isUserInOrganization(Long userId, Integer orgNumber) {
        return userOrgRepository.existsByUserIdAndOrgNumber(userId, orgNumber);
    }
}
