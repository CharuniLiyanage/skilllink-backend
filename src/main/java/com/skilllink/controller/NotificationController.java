package com.skilllink.controller;

import com.skilllink.entity.Notification;
import com.skilllink.entity.User;
import com.skilllink.repository.NotificationRepository;
import com.skilllink.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ==================== Get Notifications ====================

    @GetMapping
    public ResponseEntity<?> getNotifications(
            Authentication authentication
    ) {

        String email = authentication.getName();

        Optional<User> user =
                userRepository.findByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                user.get().getId()
                        );

        List<Map<String, Object>> response =
                notifications.stream()
                        .map(this::createNotificationResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // ==================== Unread Count ====================

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication
    ) {

        String email = authentication.getName();

        Optional<User> user =
                userRepository.findByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        long count =
                notificationRepository
                        .countByUserIdAndIsReadFalse(
                                user.get().getId()
                        );

        return ResponseEntity.ok(
                Map.of("count", count)
        );
    }

    // ==================== Mark as Read ====================

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String email = authentication.getName();

        Optional<User> user =
                userRepository.findByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        Optional<Notification> optionalNotification =
                notificationRepository.findById(id);

        if (optionalNotification.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Notification not found");
        }

        Notification notification =
                optionalNotification.get();

        if (!notification.getUser()
                .getId()
                .equals(user.get().getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only update your own notifications");
        }

        notification.setRead(true);

        Notification updatedNotification =
                notificationRepository.save(notification);

        return ResponseEntity.ok(
                createNotificationResponse(
                        updatedNotification
                )
        );
    }

    // ==================== Response ====================

    private Map<String, Object> createNotificationResponse(
            Notification notification
    ) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "id",
                notification.getId()
        );

        response.put(
                "title",
                notification.getTitle()
        );

        response.put(
                "message",
                notification.getMessage()
        );

        response.put(
                "isRead",
                notification.isRead()
        );

        response.put(
                "createdAt",
                notification.getCreatedAt()
        );

        return response;
    }
}