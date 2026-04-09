package com.example.InternalControl.service.notification;

import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for notification delivery management.
 */
public interface NotificationDeliveryService {

    /**
     * Get delivery records for a notification.
     *
     * @param notificationId the notification ID
     * @return list of delivery records
     */
    List<NotificationDeliveryResponse> getDeliveriesByNotificationId(Long notificationId);

    /**
     * Get delivery status for specific channel.
     *
     * @param notificationId the notification ID
     * @param channel        the delivery channel
     * @return the delivery record
     */
    NotificationDeliveryResponse getDeliveryByChannel(Long notificationId, DeliveryChannel channel);

    /**
     * Retry failed deliveries.
     *
     * @param notificationId the notification ID
     * @param channel        optional channel to retry (null for all channels)
     */
    void retryDeliveries(Long notificationId, DeliveryChannel channel);

    /**
     * Get all pending deliveries.
     *
     * @param pageable pagination info
     * @return page of pending deliveries
     */
    Page<NotificationDeliveryResponse> getPendingDeliveries(Pageable pageable);

    /**
     * Get all failed deliveries.
     *
     * @param pageable pagination info
     * @return page of failed deliveries
     */
    Page<NotificationDeliveryResponse> getFailedDeliveries(Pageable pageable);
}
