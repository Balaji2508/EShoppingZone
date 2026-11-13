package com.eshoppingzone.order.service;

import com.eshoppingzone.order.dto.CreateOrderRequest;
import com.eshoppingzone.order.dto.MerchantRevenueResponse;
import com.eshoppingzone.order.dto.OrderResponse;
import com.eshoppingzone.order.dto.UpdateOrderStatusRequest;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long orderId);
    List<OrderResponse> getOrdersByUserId(Long userId);
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByMerchantId(Long merchantId);
    MerchantRevenueResponse getMerchantRevenue(Long merchantId);
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    // Lifecycle & Delivery Methods
    OrderResponse acceptOrder(Long orderId, Long merchantId);
    OrderResponse rejectOrder(Long orderId, Long merchantId, String reason);
    OrderResponse packOrder(Long orderId, Long merchantId);
    OrderResponse markReadyForDelivery(Long orderId, Long merchantId);
    OrderResponse assignDeliveryPartner(Long orderId, Long merchantId, Long deliveryPartnerId, String deliveryPartnerName, String deliveryPartnerPhone);
    List<OrderResponse> getOrdersByDeliveryPartnerId(Long deliveryPartnerId);
    OrderResponse updateDeliveryStatus(Long orderId, Long deliveryPartnerId, String status);
    List<Long> getDeliveredProductIdsForUser(Long userId);
    OrderResponse cancelOrder(Long orderId, Long userId, String reason);
}
