package com.example.InternalControl.repository.notification;

import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationId(Long notificationId);

    Optional<NotificationDelivery> findByNotificationIdAndDeliveryChannel(Long notificationId, DeliveryChannel channel);

    List<NotificationDelivery> findByNotificationIdAndDeliveryStatus(Long notificationId, DeliveryStatus status);

    Page<NotificationDelivery> findByDeliveryStatus(DeliveryStatus status, Pageable pageable);
}
