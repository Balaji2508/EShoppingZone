package com.eshoppingzone.notification.consumer;

import com.eshoppingzone.notification.config.RabbitMQConfig;
import com.eshoppingzone.notification.dto.NotificationResponse;
import com.eshoppingzone.notification.dto.OrderPlacedEvent;
import com.eshoppingzone.notification.dto.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RabbitListener(queues = RabbitMQConfig.QUEUE)
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<NotificationResponse> notifications = Collections.synchronizedList(new ArrayList<>());
    private final List<String> receivedLogs = Collections.synchronizedList(new ArrayList<>());

    @RabbitHandler
    public void consumeOrderPlacedEvent(OrderPlacedEvent event) {
        logger.info("=================================================================");
        logger.info(" [RABBITMQ NOTIFICATION SERVICE] RECEIVED ORDER PLACED EVENT:");
        logger.info(" Order ID: {}", event.getOrderId());
        logger.info(" Customer ID: {}", event.getUserId());
        logger.info(" Total Amount: ₹{}", event.getTotalAmount());
        logger.info(" Payment Method: {}", event.getPaymentMethod());
        logger.info(" Payment Status: {}", event.getPaymentStatus());
        logger.info(" Items Ordered: {}", event.getItemsCount());
        logger.info(" Order Timestamp: {}", event.getOrderDate());
        logger.info("=================================================================");

        // Customer Notification
        String title = "Order #" + event.getOrderId() + " Placed Successfully!";
        String message = String.format("Your order #%d has been placed for ₹%.2f via %s. Status: %s.",
                event.getOrderId(), event.getTotalAmount(), event.getPaymentMethod(), event.getOrderStatus());

        NotificationResponse customerNotif = new NotificationResponse(
                idGenerator.getAndIncrement(),
                event.getUserId(),
                "CUSTOMER",
                event.getOrderId(),
                title,
                message,
                "ORDER_PLACED",
                false,
                LocalDateTime.now()
        );
        notifications.add(0, customerNotif);

        // Merchant Notification (Merchant ID 2)
        NotificationResponse merchantNotif = new NotificationResponse(
                idGenerator.getAndIncrement(),
                2L,
                "MERCHANT",
                event.getOrderId(),
                "New Order Received: #" + event.getOrderId(),
                String.format("You have received a new order #%d for ₹%.2f (%s). Please review and accept.",
                        event.getOrderId(), event.getTotalAmount(), event.getPaymentMethod()),
                "NEW_ORDER_RECEIVED",
                false,
                LocalDateTime.now()
        );
        notifications.add(0, merchantNotif);

        String record = String.format("[%s] Order #%d confirmed for User #%d | Total: ₹%.2f | Payment: %s (%s)",
                LocalDateTime.now(), event.getOrderId(), event.getUserId(), event.getTotalAmount(),
                event.getPaymentMethod(), event.getPaymentStatus());
        receivedLogs.add(0, record);
    }

    @RabbitHandler
    public void consumeOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        logger.info("=================================================================");
        logger.info(" [RABBITMQ NOTIFICATION SERVICE] RECEIVED ORDER STATUS UPDATE:");
        logger.info(" Order ID: {}", event.getOrderId());
        logger.info(" Customer ID: {}", event.getUserId());
        logger.info(" Status Transition: {} -> {}", event.getOldStatus(), event.getNewStatus());
        logger.info(" Payment Status: {}", event.getPaymentStatus());
        logger.info("=================================================================");

        String status = event.getNewStatus();
        String title = "Order #" + event.getOrderId() + " Update: " + status;
        String message;

        switch (status) {
            case "ACCEPTED":
                message = "Your order #" + event.getOrderId() + " has been accepted by the seller and is being processed.";
                break;
            case "REJECTED":
                message = "Your order #" + event.getOrderId() + " was rejected by the seller. If paid via wallet, a refund has been issued.";
                break;
            case "PACKING":
                message = "Your order #" + event.getOrderId() + " is currently being packed by the seller.";
                break;
            case "READY_FOR_DELIVERY":
                message = "Your order #" + event.getOrderId() + " is packed and ready for delivery partner pickup.";
                break;
            case "ASSIGNED_TO_DELIVERY":
                message = "Your order #" + event.getOrderId() + " has been assigned to a delivery partner.";
                // Also notify delivery partner (ID: 3)
                NotificationResponse dpNotif = new NotificationResponse(
                        idGenerator.getAndIncrement(),
                        3L,
                        "DELIVERY_MAN",
                        event.getOrderId(),
                        "New Delivery Assigned: Order #" + event.getOrderId(),
                        "Order #" + event.getOrderId() + " is ready for pickup and delivery. Please accept or pick up.",
                        "DELIVERY_ASSIGNED",
                        false,
                        LocalDateTime.now()
                );
                notifications.add(0, dpNotif);
                break;
            case "DELIVERY_ACCEPTED":
                message = "Delivery partner has accepted your order #" + event.getOrderId() + " and will pick it up shortly.";
                break;
            case "PICKED_UP":
                message = "Your order #" + event.getOrderId() + " has been picked up by the delivery partner.";
                break;
            case "OUT_FOR_DELIVERY":
                message = "Your order #" + event.getOrderId() + " is out for delivery! Please be ready to receive it.";
                break;
            case "DELIVERED":
                message = "Your order #" + event.getOrderId() + " has been delivered successfully! Please leave a review on the product page.";
                break;
            default:
                message = String.format("Your order #%d status has been updated to %s (Payment: %s).",
                        event.getOrderId(), event.getNewStatus(), event.getPaymentStatus());
        }

        NotificationResponse notif = new NotificationResponse(
                idGenerator.getAndIncrement(),
                event.getUserId(),
                "CUSTOMER",
                event.getOrderId(),
                title,
                message,
                "ORDER_STATUS_CHANGED",
                false,
                LocalDateTime.now()
        );
        notifications.add(0, notif);

        String record = String.format("[%s] Order #%d status updated to %s for User #%d",
                LocalDateTime.now(), event.getOrderId(), event.getNewStatus(), event.getUserId());
        receivedLogs.add(0, record);
    }

    public NotificationResponse createManualNotification(com.eshoppingzone.notification.dto.NotificationRequest req) {
        NotificationResponse notif = new NotificationResponse(
                idGenerator.getAndIncrement(),
                req.getUserId(),
                req.getUserRole() != null ? req.getUserRole() : "CUSTOMER",
                req.getOrderId(),
                req.getTitle() != null ? req.getTitle() : "Notification",
                req.getMessage(),
                req.getType() != null ? req.getType() : "INFO",
                false,
                LocalDateTime.now()
        );
        notifications.add(0, notif);
        return notif;
    }

    public List<NotificationResponse> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return notifications.stream()
                .filter(n -> userId.equals(n.getUserId()))
                .collect(Collectors.toList());
    }

    public void markAsRead(Long id) {
        notifications.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .ifPresent(n -> n.setRead(true));
    }

    public void deleteNotification(Long id) {
        notifications.removeIf(n -> id.equals(n.getId()));
    }

    public void clearNotificationsForUser(Long userId) {
        notifications.removeIf(n -> userId.equals(n.getUserId()));
    }

    public List<String> getReceivedLogs() {
        return new ArrayList<>(receivedLogs);
    }
}
