package com.example.InternalControl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitingFilter.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void doFilterInternal_WithinRateLimit_AllowsRequest() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).addHeader(eq("X-Rate-Limit-Remaining"), anyString());
    }

    @Test
    void doFilterInternal_ExceedsRateLimit_Returns429() throws ServletException, IOException {
        // Given - auth endpoint with very low limit (5 requests)
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Exhaust the bucket with 6 requests
        for (int i = 0; i < 6; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Then
        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).addHeader(eq("X-Rate-Limit-Retry-After-Seconds"), anyString());
    }

    @Test
    void doFilterInternal_WithXForwardedFor_UsesForwardedIp() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthEndpoints_StricterLimit() throws ServletException, IOException {
        // Given - auth endpoint
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When - make requests
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Then - should allow exactly 5 requests (auth limit)
        verify(filterChain, times(5)).doFilter(request, response);
    }
}
