package com.example.InternalControl.controller.notification;

import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.service.notification.NotificationDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for notification delivery management.
 * Provides endpoints for tracking and managing notification delivery status.
 */
@RestController
@RequestMapping("/api/v1/notifications/delivery")
@Tag(name = "Notification Delivery", description = "Notification delivery tracking API")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryController {

    private final NotificationDeliveryService deliveryService;

    @Operation(summary = "Get delivery status for a notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved delivery records"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<NotificationDeliveryResponse>> getDeliveriesByNotification(
            @Parameter(description = "Identifier of the notificationId")
            @PathVariable Long notificationId) {
        log.info("Getting deliveries for notification: {}", notificationId);
        return ResponseEntity.ok(deliveryService.getDeliveriesByNotificationId(notificationId));
    }

    @Operation(summary = "Get delivery status for specific channel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved delivery record"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Delivery record not found")
    })
    @GetMapping("/{notificationId}/status/{channel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<NotificationDeliveryResponse> getDeliveryByChannel(
            @Parameter(description = "Identifier of the notificationId")
            @PathVariable Long notificationId,
            @PathVariable DeliveryChannel channel) {
        log.info("Getting delivery for notification {} and channel {}", notificationId, channel);
        return ResponseEntity.ok(deliveryService.getDeliveryByChannel(notificationId, channel));
    }

    @Operation(summary = "Retry failed deliveries")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Retry accepted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PostMapping("/{notificationId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> retryDeliveries(
            @Parameter(description = "Identifier of the notificationId")
            @PathVariable Long notificationId,
            @RequestParam(required = false) DeliveryChannel channel) {
        log.info("Retrying deliveries for notification: {}, channel: {}", notificationId, channel);
        deliveryService.retryDeliveries(notificationId, channel);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Get all pending deliveries (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved pending deliveries"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDeliveryResponse>> getPendingDeliveries(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting pending deliveries");
        return ResponseEntity.ok(deliveryService.getPendingDeliveries(pageable));
    }

    @Operation(summary = "Get all failed deliveries (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved failed deliveries"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDeliveryResponse>> getFailedDeliveries(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting failed deliveries");
        return ResponseEntity.ok(deliveryService.getFailedDeliveries(pageable));
    }
}
