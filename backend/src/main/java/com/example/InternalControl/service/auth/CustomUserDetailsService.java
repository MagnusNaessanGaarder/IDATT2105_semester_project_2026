package com.example.InternalControl.service.auth;

import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading user details.
 * Used by Spring Security during authentication.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final AppUserRepository userRepository;
    private final UserOrganizationRepository userOrgRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LOGGER.debug("Loading user: {}", email);
        
        AppUser user = userRepository.findByEmailWithCredentials(email)
                .orElseThrow(() -> {
                    LOGGER.warn("User not found: {}", email);
                    return new UsernameNotFoundException("Invalid email or password");
                });

        if (user.getLocalCredential() == null) {
            LOGGER.warn("User {} has no password-based login", email);
            throw new UsernameNotFoundException("Invalid email or password");
        }

        if (user.isLocked()) {
            LOGGER.warn("User {} is locked until {}", email, user.getLocalCredential().getLockedUntil());
            throw new UsernameNotFoundException("Account is temporarily locked. Try again later.");
        }

        if (!user.isActive()) {
            LOGGER.warn("User {} is disabled", email);
            throw new UsernameNotFoundException("Account is disabled");
        }

        // Fetch all roles from user_organization_role table
        List<UserOrganization> userOrgs = userOrgRepository.findActiveOrganizationsByUserId(user.getUserId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        for (UserOrganization userOrg : userOrgs) {
            List<UserOrganizationRole> roles = userOrgRoleRepository.findByUserOrganization(
                    userOrg.getUser().getUserId(),
                    userOrg.getOrganization().getOrgNumber()
            );
            
            for (UserOrganizationRole userOrgRole : roles) {
                String roleName = userOrgRole.getRole().getRoleName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                LOGGER.debug("User {} has role: {}", email, roleName);
            }
        }
        
        // If no roles found, assign default EMPLOYEE role
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            LOGGER.warn("User {} has no roles, assigned default EMPLOYEE role", email);
        }

        LOGGER.info("User {} authenticated with {} roles", email, authorities.size());

        return new CustomUserDetails(
                user.getUserId(),
                user.getEmail(),
                user.getLocalCredential().getPasswordHash(),
                user.isActive(),
                true,
                true,
                !user.isLocked(),
                authorities
        );
    }
}
