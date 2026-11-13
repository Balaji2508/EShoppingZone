package com.eshoppingzone.notification.dto;

public class NotificationRequest {
    private Long userId;
    private String userRole;
    private Long orderId;
    private String title;
    private String message;
    private String type;

    public NotificationRequest() {
    }

    public NotificationRequest(Long userId, String userRole, Long orderId, String title, String message, String type) {
        this.userId = userId;
        this.userRole = userRole;
        this.orderId = orderId;
        this.title = title;
        this.message = message;
        this.type = type;
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
}
