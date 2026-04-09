package com.example.InternalControl.repository.notification;

import com.example.InternalControl.model.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.orgNumber = :orgNumber ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndOrgNumber(@Param("userId") Long userId, @Param("orgNumber") Integer orgNumber);

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.orgNumber = :orgNumber AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserIdAndOrgNumber(@Param("userId") Long userId, @Param("orgNumber") Integer orgNumber);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.orgNumber = :orgNumber AND n.isRead = false")
    long countByUserIdAndOrgNumberAndIsReadFalse(@Param("userId") Long userId, @Param("orgNumber") Integer orgNumber);
}