package com.example.InternalControl.controller.notification;

import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.notification.NotificationDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
@WebMvcTest(controllers = NotificationDeliveryController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class NotificationDeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationDeliveryService deliveryService;

    @MockBean
    private JwtService jwtService;

    private NotificationDeliveryResponse mockDelivery;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        mockDelivery = NotificationDeliveryResponse.builder()
                .deliveryId(1L)
                .notificationId(1L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.SENT)
                .attemptedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getDeliveryStatus_AsAdmin_ReturnsOk() throws Exception {
        List<NotificationDeliveryResponse> deliveries = Arrays.asList(mockDelivery);
        when(deliveryService.getDeliveriesByNotificationId(anyLong())).thenReturn(deliveries);

        mockMvc.perform(get("/api/v1/notifications/delivery/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId").value(1));
    }

    @Test
    void getDeliveryStatus_AsManager_ReturnsOk() throws Exception {
        List<NotificationDeliveryResponse> deliveries = Arrays.asList(mockDelivery);
        when(deliveryService.getDeliveriesByNotificationId(anyLong())).thenReturn(deliveries);

        mockMvc.perform(get("/api/v1/notifications/delivery/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getDeliveryStatus_ReturnsOk() throws Exception {
        List<NotificationDeliveryResponse> deliveries = Arrays.asList(mockDelivery);
        when(deliveryService.getDeliveriesByNotificationId(anyLong())).thenReturn(deliveries);

        mockMvc.perform(get("/api/v1/notifications/delivery/1"))
                .andExpect(status().isOk());
    }

    @Test
    void retryFailedDeliveries_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/delivery/1/retry"))
                .andExpect(status().isAccepted());
    }

    @Test
    void retryFailedDeliveries_ReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/delivery/1/retry"))
                .andExpect(status().isAccepted());
    }

    @Test
    void getPendingDeliveries_AsAdmin_ReturnsOk() throws Exception {
        List<NotificationDeliveryResponse> deliveries = Arrays.asList(mockDelivery);
        Page<NotificationDeliveryResponse> page = new PageImpl<>(deliveries);
        when(deliveryService.getPendingDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/delivery/pending"))
                .andExpect(status().isOk());
    }

    @Test
    void getFailedDeliveries_AsAdmin_ReturnsOk() throws Exception {
        NotificationDeliveryResponse failedDelivery = NotificationDeliveryResponse.builder()
                .deliveryId(1L)
                .notificationId(1L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.FAILED)
                .attemptedAt(LocalDateTime.now())
                .build();
        List<NotificationDeliveryResponse> deliveries = Arrays.asList(failedDelivery);
        Page<NotificationDeliveryResponse> page = new PageImpl<>(deliveries);
        when(deliveryService.getFailedDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/delivery/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryStatus").value("FAILED"));
    }
}
