package com.example.InternalControl.controller.notification;

import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get user's notifications")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, orgNumber));
    }

    @Operation(summary = "Get unread notifications count")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread count"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber) {
        Long userId = userDetails.getUserId();
        Long count = notificationService.getUnreadCount(userId, orgNumber);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get specific notification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved notification"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> getNotification(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(notificationService.getNotification(id, userId));
    }

    @Operation(summary = "Mark notification as read")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notification marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "All notifications marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber) {
        Long userId = userDetails.getUserId();
        notificationService.markAllAsRead(userId, orgNumber);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete notification")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notification deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Identifier of the id")
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}
