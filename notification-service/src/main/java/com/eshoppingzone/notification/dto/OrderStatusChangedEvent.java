package com.eshoppingzone.notification.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderStatusChangedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;
    private String oldStatus;
    private String newStatus;
    private String paymentStatus;
    private LocalDateTime timestamp;

    public OrderStatusChangedEvent() {
    }

    public OrderStatusChangedEvent(Long orderId, Long userId, String oldStatus, String newStatus, String paymentStatus, LocalDateTime timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.paymentStatus = paymentStatus;
        this.timestamp = timestamp;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OrderStatusChangedEvent{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
