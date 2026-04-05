package com.example.InternalControl.service.user;

import com.example.InternalControl.repository.user.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-organization membership operations.
 */
@Service
@RequiredArgsConstructor
public class UserOrganizationService {

    private final UserOrganizationRepository userOrgRepository;

    @Transactional(readOnly = true)
    public boolean isUserInOrganization(Long userId, Integer orgNumber) {
        return userOrgRepository.existsByUserIdAndOrgNumber(userId, orgNumber);
    }
}
