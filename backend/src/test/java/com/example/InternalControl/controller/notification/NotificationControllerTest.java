package com.example.InternalControl.controller.notification;

import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = NotificationController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        mockNotification = Notification.builder()
                .notificationId(1L)
                .orgNumber(937219997)
                .title("Test Notification")
                .bodyText("Test body")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test

    void getNotifications_Authenticated_ReturnsOk() throws Exception {
        List<Notification> notifications = Arrays.asList(mockNotification);
        when(notificationService.getUserNotifications(anyLong(), anyInt())).thenReturn(notifications);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test
    void getNotifications_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test

    void getUnreadCount_Authenticated_ReturnsOk() throws Exception {
        when(notificationService.getUnreadCount(anyLong(), anyInt())).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test

    void markAsRead_ValidId_ReturnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/1/read"))
                .andExpect(status().isNoContent());
    }

    @Test

    void markAllAsRead_ReturnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test

    void getNotificationById_Existing_ReturnsOk() throws Exception {
        when(notificationService.getNotification(anyLong(), anyLong())).thenReturn(mockNotification);

        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(1));
    }

    @Test

    void deleteNotification_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/1"))
                .andExpect(status().isNoContent());
    }
}
