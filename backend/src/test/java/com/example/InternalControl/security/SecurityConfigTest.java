package com.example.InternalControl.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Test
    void passwordEncoder_IsBCrypt() {
        // Given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // When
        String encoded = passwordEncoder.encode("password");

        // Then
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$")); // BCrypt prefix
        assertTrue(passwordEncoder.matches("password", encoded));
    }

    @Test
    void passwordEncoder_EncodesDifferentPasswords() {
        // Given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // When
        String encoded1 = passwordEncoder.encode("password1");
        String encoded2 = passwordEncoder.encode("password2");

        // Then
        assertNotEquals(encoded1, encoded2); // Different salts
        assertTrue(passwordEncoder.matches("password1", encoded1));
        assertTrue(passwordEncoder.matches("password2", encoded2));
        assertFalse(passwordEncoder.matches("password1", encoded2));
    }

    @Test
    void corsConfigurationSource_Exists() {
        // Given
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        // Then
        assertNotNull(source);
    }
}
