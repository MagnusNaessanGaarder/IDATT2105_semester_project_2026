package com.example.InternalControl.controller.notification;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.service.notification.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationDeliveryController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class NotificationDeliveryControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationDeliveryService deliveryService;

    private static final String BASE_URL = "/api/v1/notifications/delivery";

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getDeliveriesByNotification_AsAdmin_ReturnsOk() throws Exception {
        // Given
        NotificationDeliveryResponse delivery = NotificationDeliveryResponse.builder()
                .deliveryId(1L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.SENT)
                .build();

        when(deliveryService.getDeliveriesByNotificationId(1L)).thenReturn(List.of(delivery));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId").value(1));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void retryFailedDeliveries_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(deliveryService).retryDeliveries(1L, null);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/1/retry"))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getDeliveries_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isForbidden());
    }
}
