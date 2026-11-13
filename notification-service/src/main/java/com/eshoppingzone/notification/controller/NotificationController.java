package com.eshoppingzone.notification.controller;

import com.eshoppingzone.notification.consumer.OrderEventConsumer;
import com.eshoppingzone.notification.dto.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final OrderEventConsumer orderEventConsumer;

    public NotificationController(OrderEventConsumer orderEventConsumer) {
        this.orderEventConsumer = orderEventConsumer;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@RequestBody com.eshoppingzone.notification.dto.NotificationRequest request) {
        NotificationResponse response = orderEventConsumer.createManualNotification(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(orderEventConsumer.getAllNotifications());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderEventConsumer.getNotificationsByUserId(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        orderEventConsumer.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read", "id", String.valueOf(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        orderEventConsumer.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted", "id", String.valueOf(id)));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearNotificationsForUser(@PathVariable Long userId) {
        orderEventConsumer.clearNotificationsForUser(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared for user"));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getLogs() {
        return ResponseEntity.ok(orderEventConsumer.getReceivedLogs());
    }
}
