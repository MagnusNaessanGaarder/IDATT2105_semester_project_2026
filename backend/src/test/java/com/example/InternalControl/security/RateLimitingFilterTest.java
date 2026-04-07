package com.example.InternalControl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    void doFilterInternal_FirstRequest_AllowsRequest() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilterInternal_MultipleRequestsFromSameIP_WithinLimit_AllowsRequests() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");

        // When - 5 requests within limit
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Then
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ExceedsRateLimit_ReturnsTooManyRequests() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // When - Exceed limit
        for (int i = 0; i < 150; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Then
        verify(response, atLeastOnce()).setStatus(429);
    }

    @Test
    void doFilterInternal_DifferentIPs_TrackedSeparately() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn("192.168.1.2");
        when(request2.getRequestURI()).thenReturn("/api/v1/test");

        // When
        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            rateLimitingFilter.doFilterInternal(request2, response, filterChain);
        }

        // Then - Both should be allowed as they're tracked separately
        verify(filterChain, times(100)).doFilter(any(), eq(response));
    }

    @Test
    void doFilterInternal_NullRemoteAddr_UsesUnknown() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/test");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then - Should not throw exception
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AddsRateLimitHeaders() throws ServletException, IOException {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, atLeastOnce()).addHeader(eq("X-Rate-Limit-Remaining"), any());
    }
}
