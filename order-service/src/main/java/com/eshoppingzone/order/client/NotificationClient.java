package com.eshoppingzone.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://localhost:8086}")
    private String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationFallback")
    public void sendNotification(Long userId, String userRole, Long orderId, String title, String message, String type) {
        try {
            String url = notificationServiceUrl + "/api/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("userRole", userRole != null ? userRole : "CUSTOMER");
            body.put("orderId", orderId);
            body.put("title", title);
            body.put("message", message);
            body.put("type", type);
            body.put("read", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, entity, Map.class);
            logger.info("Sent sync notification to user #{}, title: {}", userId, title);
        } catch (Exception e) {
            logger.warn("Could not send sync notification to notification-service: {}", e.getMessage());
        }
    }

    public void sendNotificationFallback(Long userId, String userRole, Long orderId, String title, String message, String type, Throwable t) {
        logger.warn("[CIRCUIT BREAKER] Notification Service is unavailable (User ID #{}, Title: '{}'): {}", userId, title, t.getMessage());
    }
}
