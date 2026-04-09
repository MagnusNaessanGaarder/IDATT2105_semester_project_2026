package com.example.InternalControl.config;
import com.example.InternalControl.AbstractIntegrationTest;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SecurityConfig.
 * Verifies security beans are properly configured.
 */

@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
        }
)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("Should configure security filter chain")
    void shouldConfigureSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    @DisplayName("Should configure BCrypt password encoder")
    void shouldConfigureBcryptPasswordEncoder() {
        // Then
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.getClass().getSimpleName()).contains("BCrypt");
    }

    @Test
    @DisplayName("Should encode and verify passwords")
    void shouldEncodeAndVerifyPasswords() {
        // Given
        String rawPassword = "TestPassword123!";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("Should configure authentication provider")
    void shouldConfigureAuthenticationProvider() {
        assertThat(authenticationProvider).isNotNull();
    }

    @Test
    @DisplayName("Should configure authentication manager")
    void shouldConfigureAuthenticationManager() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @DisplayName("Should configure CORS configuration source")
    void shouldConfigureCorsConfigurationSource() {
        assertThat(corsConfigurationSource).isNotNull();
    }
}
