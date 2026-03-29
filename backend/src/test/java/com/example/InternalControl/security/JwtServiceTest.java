package com.example.InternalControl.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 *
 * @author TriTacLe
 * @since 1.0
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);

        testUser = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token")
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Should extract correct username from token")
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Token should be valid for correct user")
    void isTokenValid_shouldReturnTrueForCorrectUser() {
        String token = jwtService.generateAccessToken(testUser);

        assertTrue(jwtService.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("Should generate different access and refresh tokens")
    void shouldGenerateDifferentTokenTypes() {
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertNotEquals(accessToken, refreshToken);
    }
}
