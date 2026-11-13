package com.eshoppingzone.notification.dto;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private Long userId;
    private String userRole; // CUSTOMER, MERCHANT, DELIVERY_MAN, ADMIN
    private Long orderId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime timestamp;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, Long userId, String userRole, Long orderId, String title, String message, String type, boolean isRead, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.userRole = userRole != null ? userRole : "CUSTOMER";
        this.orderId = orderId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
