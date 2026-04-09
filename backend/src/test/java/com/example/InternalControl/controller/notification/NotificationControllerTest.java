package com.example.InternalControl.controller.notification;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.isOneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/notifications";

    @BeforeEach
    void setUp() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth == null) {
            return;
        }
        CustomUserDetails userDetails = new CustomUserDetails(
                1L, existingAuth.getName(), "password", existingAuth.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getNotifications_AsAuthenticatedUser_ReturnsOk() throws Exception {
        // Given
        Notification notification = Notification.builder()
                .notificationId(1L)
                .title("Test Notification")
                .notificationType(NotificationType.GENERAL)
                .build();

        when(notificationService.getUserNotifications(anyLong(), eq(ORG_NUMBER)))
                .thenReturn(List.of(notification));

        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test
    void getNotifications_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().is(isOneOf(401, 403)));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getUnreadCount_AsAuthenticatedUser_ReturnsOk() throws Exception {
        // Given
        when(notificationService.getUnreadCount(anyLong(), eq(ORG_NUMBER)))
                .thenReturn(5L);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/unread-count")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getNotification_AsAuthenticatedUser_ReturnsOk() throws Exception {
        // Given
        Notification notification = Notification.builder()
                .notificationId(1L)
                .title("Test Notification")
                .build();

        when(notificationService.getNotification(1L, 1L)).thenReturn(notification);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void markAsRead_AsAuthenticatedUser_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(notificationService).markAsRead(1L, 1L);

        // When & Then
        mockMvc.perform(put(BASE_URL + "/1/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void markAllAsRead_AsAuthenticatedUser_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(notificationService).markAllAsRead(anyLong(), eq(ORG_NUMBER));

        // When & Then
        mockMvc.perform(put(BASE_URL + "/read-all")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void deleteNotification_AsAuthenticatedUser_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(notificationService).deleteNotification(1L, 1L);

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }
}
