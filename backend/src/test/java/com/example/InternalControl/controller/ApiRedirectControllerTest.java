package com.example.InternalControl.controller;

import com.example.InternalControl.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = ApiRedirectController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ApiRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @Test
    void redirectToVersionedApi_ReturnsPermanentRedirect() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(redirectedUrl("/api/v1/users"));
    }

    @Test
    void redirectToVersionedApi_WithQueryParams_ReturnsPermanentRedirect() throws Exception {
        mockMvc.perform(get("/api/users?page=0"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(redirectedUrl("/api/v1/users?page=0"));
    }
}
