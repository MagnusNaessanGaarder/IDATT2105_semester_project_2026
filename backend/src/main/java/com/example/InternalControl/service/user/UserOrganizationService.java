package com.example.InternalControl.service.user;

import com.example.InternalControl.model.organization.UserOrganizationRole;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @Transactional(readOnly = true)
    public boolean isUserInOrganization(Long userId, Integer orgNumber) {
        return userOrgRepository.existsByUserIdAndOrgNumber(userId, orgNumber);
    }

    @Transactional(readOnly = true)
    public boolean isUserManagerOrAdmin(Long userId, Integer orgNumber) {
        List<UserOrganizationRole> roles = userOrgRoleRepository.findByUserOrganization(userId, orgNumber);
        return roles.stream()
            .anyMatch(role -> {
                String roleName = role.getRole().getRoleName();
                return "ADMIN".equals(roleName) || "MANAGER".equals(roleName);
            });
    }
}
