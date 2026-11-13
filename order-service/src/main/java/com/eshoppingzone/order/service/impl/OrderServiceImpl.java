package com.eshoppingzone.order.service.impl;

import com.eshoppingzone.order.client.CartClient;
import com.eshoppingzone.order.client.NotificationClient;
import com.eshoppingzone.order.client.PaymentClient;
import com.eshoppingzone.order.client.ProductClient;
import com.eshoppingzone.order.client.WalletClient;
import com.eshoppingzone.order.config.RabbitMQConfig;
import com.eshoppingzone.order.dto.*;
import com.eshoppingzone.order.entity.Order;
import com.eshoppingzone.order.entity.OrderItem;
import com.eshoppingzone.order.exception.BadRequestException;
import com.eshoppingzone.order.exception.ResourceNotFoundException;
import com.eshoppingzone.order.repository.OrderRepository;
import com.eshoppingzone.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final WalletClient walletClient;
    private final NotificationClient notificationClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartClient cartClient,
                            ProductClient productClient,
                            PaymentClient paymentClient,
                            WalletClient walletClient,
                            NotificationClient notificationClient,
                            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.walletClient = walletClient;
        this.notificationClient = notificationClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Initiating checkout for user ID: {}, payment method: {}", request.getUserId(), request.getPaymentMethod());

        // 1. Fetch Cart
        CartDto cart = cartClient.getCart(request.getUserId());
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            logger.warn("Order failed: Cart is empty for user ID: {}", request.getUserId());
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        Double totalAmount = cart.getTotalAmount();
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "COD";

        // 2. Process Payment via dedicated Payment Service
        Map<String, Object> paymentResult = paymentClient.processPayment(request, totalAmount, null);
        String paymentStatus = (String) paymentResult.getOrDefault("paymentStatus", "PAID");

        // 3. Deduct Stock for each cart item
        for (CartItemDto item : cart.getItems()) {
            logger.info("Reducing stock for product ID: {}, Quantity: {}", item.getProductId(), item.getQuantity());
            productClient.reduceStock(item.getProductId(), item.getQuantity());
        }

        // 4. Save Order and OrderItems
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        order.setOrderStatus("CONFIRMED".equals(paymentStatus) || "PAID".equals(paymentStatus) ? "CONFIRMED" : "PLACED");
        order.setShippingAddress(request.getShippingAddress());
        order.setDeliveryStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDto item : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(item.getSubtotal());
            orderItem.setImageUrl(item.getImageUrl());

            // Fetch merchant details for item
            ProductDto p = productClient.getProduct(item.getProductId());
            if (p != null) {
                orderItem.setMerchantId(p.getMerchantId() != null ? p.getMerchantId() : 2L);
                orderItem.setMerchantName(p.getMerchantName() != null ? p.getMerchantName() : "Alice Merchant");
            } else {
                orderItem.setMerchantId(2L);
                orderItem.setMerchantName("Alice Merchant");
            }

            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with ID: {}", savedOrder.getId());

        // 5. Clear Cart
        cartClient.clearCart(request.getUserId());

        // 6. Publish OrderPlacedEvent to RabbitMQ & send direct sync notifications
        try {
            OrderPlacedEvent event = new OrderPlacedEvent(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getPaymentMethod(),
                    savedOrder.getPaymentStatus(),
                    savedOrder.getOrderStatus(),
                    savedOrder.getOrderDate(),
                    savedOrder.getItems().size()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            logger.info("Published OrderPlacedEvent to RabbitMQ: {}", event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderPlacedEvent to RabbitMQ: {}", e.getMessage());
        }

        // Direct sync notifications for Customer, Merchant, and Admin
        notificationClient.sendNotification(savedOrder.getUserId(), "CUSTOMER", savedOrder.getId(),
                "Order Placed Successfully",
                "Your order #" + savedOrder.getId() + " has been placed successfully. Payment: " + savedOrder.getPaymentMethod(),
                "ORDER_PLACED");

        notificationClient.sendNotification(2L, "MERCHANT", savedOrder.getId(),
                "New Order Received",
                "New order #" + savedOrder.getId() + " received with " + savedOrder.getItems().size() + " items for ₹" + savedOrder.getTotalAmount(),
                "ORDER_PLACED");

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        logger.info("Fetching orders for user ID: {}", userId);
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByMerchantId(Long merchantId) {
        logger.info("Fetching orders for merchant ID: {}", merchantId);
        return orderRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantRevenueResponse getMerchantRevenue(Long merchantId) {
        logger.info("Calculating revenue for merchant ID: {}", merchantId);
        List<Order> orders = orderRepository.findByMerchantId(merchantId);

        double totalRevenue = 0.0;
        double pendingRevenue = 0.0;
        int completedOrders = 0;
        int pendingOrders = 0;
        int totalUnitsSold = 0;

        for (Order order : orders) {
            double merchantOrderTotal = 0.0;
            int merchantUnits = 0;

            for (OrderItem item : order.getItems()) {
                if (merchantId.equals(item.getMerchantId())) {
                    merchantOrderTotal += item.getSubtotal();
                    merchantUnits += item.getQuantity();
                }
            }

            totalUnitsSold += merchantUnits;

            if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus()) || "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                totalRevenue += merchantOrderTotal;
                completedOrders++;
            } else if (!"CANCELLED".equalsIgnoreCase(order.getOrderStatus()) && !"REJECTED".equalsIgnoreCase(order.getOrderStatus())) {
                pendingRevenue += merchantOrderTotal;
                pendingOrders++;
            }
        }

        List<OrderResponse> orderResponses = orders.stream().map(this::mapToResponse).collect(Collectors.toList());

        return new MerchantRevenueResponse(
                merchantId,
                totalRevenue,
                pendingRevenue,
                orders.size(),
                completedOrders,
                pendingOrders,
                totalUnitsSold,
                orderResponses
        );
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        String newStatus = request.getOrderStatus().toUpperCase();
        logger.info("Updating status for order ID: {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);

        if ("DELIVERED".equals(newStatus) && "COD_PENDING".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
        }

        Order updated = orderRepository.save(order);
        logger.info("Order status updated from {} to {}", oldStatus, newStatus);
        publishStatusChangeEvent(updated, oldStatus, newStatus);

        return mapToResponse(updated);
    }

    @Override
    public OrderResponse acceptOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("ACCEPTED");
        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, "ACCEPTED");
        return mapToResponse(updated);
    }

    @Override
    public OrderResponse rejectOrder(Long orderId, Long merchantId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required to reject an order");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Cannot reject an order that has already been delivered");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("REJECTED");
        order.setRejectionReason(reason.trim());
        order.setRejectedDate(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, "REJECTED");
        return mapToResponse(updated);
    }

    @Override
    public OrderResponse packOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("PACKING");
        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, "PACKING");
        return mapToResponse(updated);
    }

    @Override
    public OrderResponse markReadyForDelivery(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("READY_FOR_DELIVERY");
        order.setDeliveryStatus("READY_FOR_DELIVERY");
        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, "READY_FOR_DELIVERY");
        return mapToResponse(updated);
    }

    @Override
    public OrderResponse assignDeliveryPartner(Long orderId, Long merchantId, Long deliveryPartnerId, String deliveryPartnerName, String deliveryPartnerPhone) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("ASSIGNED_TO_DELIVERY");
        order.setDeliveryStatus("ASSIGNED");
        order.setDeliveryPartnerId(deliveryPartnerId != null ? deliveryPartnerId : 3L);
        order.setDeliveryPartnerName(deliveryPartnerName != null ? deliveryPartnerName : "David Delivery");
        order.setDeliveryPartnerPhone(deliveryPartnerPhone != null ? deliveryPartnerPhone : "9870001122");

        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, "ASSIGNED_TO_DELIVERY");
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDeliveryPartnerId(Long deliveryPartnerId) {
        return orderRepository.findByDeliveryPartnerIdOrderByOrderDateDesc(deliveryPartnerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateDeliveryStatus(Long orderId, Long deliveryPartnerId, String status) {
        String cleanStatus = status.toUpperCase();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        String oldStatus = order.getOrderStatus();

        if ("DELIVERY_ACCEPTED".equals(cleanStatus)) {
            order.setOrderStatus("DELIVERY_ACCEPTED");
            order.setDeliveryStatus("DELIVERY_ACCEPTED");
        } else if ("PICKED_UP".equals(cleanStatus)) {
            order.setOrderStatus("PICKED_UP");
            order.setDeliveryStatus("PICKED_UP");
        } else if ("OUT_FOR_DELIVERY".equals(cleanStatus)) {
            order.setOrderStatus("OUT_FOR_DELIVERY");
            order.setDeliveryStatus("OUT_FOR_DELIVERY");
        } else if ("DELIVERED".equals(cleanStatus)) {
            order.setOrderStatus("DELIVERED");
            order.setDeliveryStatus("DELIVERED");
            if ("COD_PENDING".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("PAID");
            }
        } else if ("DECLINED".equals(cleanStatus)) {
            order.setDeliveryStatus("DECLINED");
            order.setOrderStatus("READY_FOR_DELIVERY"); // Revert to ready so merchant can re-assign
        } else {
            throw new BadRequestException("Invalid delivery status: " + status);
        }

        Order updated = orderRepository.save(order);
        publishStatusChangeEvent(updated, oldStatus, order.getOrderStatus());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getDeliveredProductIdsForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        return orders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus()))
                .flatMap(o -> o.getItems().stream())
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        logger.info("User {} attempting to cancel order ID: {}", userId, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        String currentStatus = order.getOrderStatus();
        if ("DELIVERED".equalsIgnoreCase(currentStatus) || "CANCELLED".equalsIgnoreCase(currentStatus) || "REJECTED".equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException("Cannot cancel an order that is already " + currentStatus);
        }
        if ("OUT_FOR_DELIVERY".equalsIgnoreCase(currentStatus) || "PICKED_UP".equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException("Order cannot be cancelled because it is already out for delivery");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("CANCELLED");
        order.setDeliveryStatus("CANCELLED");
        order.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Cancelled by customer");
        order.setRejectedDate(LocalDateTime.now());

        // Refund to wallet if order was paid or payment method was WALLET/UPI/CARD
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus()) || "WALLET".equalsIgnoreCase(order.getPaymentMethod())) {
            try {
                walletClient.topup(order.getUserId(), order.getTotalAmount(), "Refund for cancelled order #" + order.getId());
                order.setPaymentStatus("REFUNDED");
                logger.info("Refunded ₹{} to user ID: {} for cancelled order #{}", order.getTotalAmount(), order.getUserId(), order.getId());
            } catch (Exception e) {
                logger.warn("Could not process automatic wallet refund for cancelled order #{}: {}", order.getId(), e.getMessage());
            }
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            try {
                productClient.restoreStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                logger.warn("Could not restore stock for product {}: {}", item.getProductId(), e.getMessage());
            }
        }

        Order saved = orderRepository.save(order);
        publishStatusChangeEvent(saved, oldStatus, "CANCELLED");

        // Notify customer and merchant
        notificationClient.sendNotification(order.getUserId(), "CUSTOMER", order.getId(),
                "Order Cancelled",
                "Your order #" + order.getId() + " has been cancelled successfully.",
                "ORDER_CANCELLED");

        notificationClient.sendNotification(2L, "MERCHANT", order.getId(),
                "Order Cancelled by Customer",
                "Order #" + order.getId() + " was cancelled by the customer.",
                "ORDER_CANCELLED");

        return mapToResponse(saved);
    }

    private void publishStatusChangeEvent(Order order, String oldStatus, String newStatus) {
        try {
            OrderStatusChangedEvent statusEvent = new OrderStatusChangedEvent(
                    order.getId(),
                    order.getUserId(),
                    oldStatus,
                    newStatus,
                    order.getPaymentStatus(),
                    LocalDateTime.now()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, statusEvent);
            logger.info("Published OrderStatusChangedEvent to RabbitMQ: {}", statusEvent);
        } catch (Exception e) {
            logger.error("Failed to publish OrderStatusChangedEvent to RabbitMQ: {}", e.getMessage());
        }

        // Direct notification to customer
        notificationClient.sendNotification(order.getUserId(), "CUSTOMER", order.getId(),
                "Order Status Updated: " + newStatus,
                "Your Order #" + order.getId() + " is now " + newStatus + ".",
                "STATUS_UPDATE");

        // Direct notification to delivery partner if assigned
        if (order.getDeliveryPartnerId() != null) {
            notificationClient.sendNotification(order.getDeliveryPartnerId(), "DELIVERY_MAN", order.getId(),
                    "Delivery Assignment Update",
                    "Order #" + order.getId() + " status is " + newStatus + " for address: " + order.getShippingAddress(),
                    "DELIVERY_UPDATE");
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getShippingAddress(),
                order.getRejectionReason(),
                order.getRejectedDate(),
                order.getDeliveryPartnerId(),
                order.getDeliveryPartnerName(),
                order.getDeliveryPartnerPhone(),
                order.getDeliveryStatus(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getPrice(),
                                item.getQuantity(),
                                item.getSubtotal(),
                                item.getImageUrl(),
                                item.getMerchantId() != null ? item.getMerchantId() : 2L,
                                item.getMerchantName() != null ? item.getMerchantName() : "Alice Merchant"
                        ))
                        .collect(Collectors.toList())
        );
    }
}
