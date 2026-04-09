package com.example.InternalControl.controller;

import com.example.InternalControl.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ApiRedirectController.
 */
@WebMvcTest(ApiRedirectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @Test
    void redirectToVersionedApi_WithNonVersionedUrl_ReturnsRedirect() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string("Location", "/api/v1/users"))
                .andExpect(header().exists("Deprecation"))
                .andExpect(header().exists("Warning"));
    }

    @Test
    void redirectToVersionedApi_WithVersionedUrl_ReturnsNotFound() throws Exception {
        // When & Then - already versioned URLs should 404 (they'll be handled by other controllers)
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectToVersionedApi_WithQueryParams_RedirectsToVersionedPath() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users").param("orgNumber", "123"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string("Location", "/api/v1/users"));
    }
}
