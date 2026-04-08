package com.example.InternalControl.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
class CustomUserDetailsTest {

    @Test
    void constructor_WithValidData_CreatesInstance() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String password = "password";
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
        );

        // When
        CustomUserDetails userDetails = new CustomUserDetails(userId, email, password, authorities);

        // Then
        assertEquals(userId, userDetails.getUserId());
        assertEquals(email, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void getAuthorities_ReturnsCorrectAuthorities() {
        // Given
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", authorities);

        // When
        Collection<? extends GrantedAuthority> result = userDetails.getAuthorities();

        // Then
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void getUsername_ReturnsEmail() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", Arrays.asList());

        // When & Then
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void isAccountNonExpired_ReturnsTrue() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", Arrays.asList());

        // When & Then
        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    void isAccountNonLocked_ReturnsTrue() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", Arrays.asList());

        // When & Then
        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    void isCredentialsNonExpired_ReturnsTrue() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", Arrays.asList());

        // When & Then
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void isEnabled_ReturnsTrue() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", Arrays.asList());

        // When & Then
        assertTrue(userDetails.isEnabled());
    }
}
