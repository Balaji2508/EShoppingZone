package com.eshoppingzone.order.controller;

import com.eshoppingzone.order.dto.CreateOrderRequest;
import com.eshoppingzone.order.dto.MerchantRevenueResponse;
import com.eshoppingzone.order.dto.OrderResponse;
import com.eshoppingzone.order.dto.UpdateOrderStatusRequest;
import com.eshoppingzone.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByMerchantId(@PathVariable Long merchantId) {
        return ResponseEntity.ok(orderService.getOrdersByMerchantId(merchantId));
    }

    @GetMapping("/merchant/{merchantId}/revenue")
    public ResponseEntity<MerchantRevenueResponse> getMerchantRevenue(@PathVariable Long merchantId) {
        return ResponseEntity.ok(orderService.getMerchantRevenue(merchantId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long orderId,
                                                           @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PutMapping("/{orderId}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable Long orderId,
                                                     @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(orderService.acceptOrder(orderId, merchantId));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by customer") : "Cancelled by customer";
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId, reason));
    }

    @PutMapping("/{orderId}/reject")
    public ResponseEntity<OrderResponse> rejectOrder(@PathVariable Long orderId,
                                                     @RequestParam(required = false) Long merchantId,
                                                     @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Out of stock or unable to fulfill");
        return ResponseEntity.ok(orderService.rejectOrder(orderId, merchantId, reason));
    }

    @PutMapping("/{orderId}/pack")
    public ResponseEntity<OrderResponse> packOrder(@PathVariable Long orderId,
                                                   @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(orderService.packOrder(orderId, merchantId));
    }

    @PutMapping("/{orderId}/ready-for-delivery")
    public ResponseEntity<OrderResponse> markReadyForDelivery(@PathVariable Long orderId,
                                                             @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(orderService.markReadyForDelivery(orderId, merchantId));
    }

    @PutMapping("/{orderId}/assign-delivery")
    public ResponseEntity<OrderResponse> assignDeliveryPartner(@PathVariable Long orderId,
                                                               @RequestParam(required = false) Long merchantId,
                                                               @RequestBody Map<String, Object> body) {
        Long partnerId = body.get("deliveryPartnerId") != null ? Long.valueOf(body.get("deliveryPartnerId").toString()) : 3L;
        String partnerName = (String) body.getOrDefault("deliveryPartnerName", "David Delivery");
        String partnerPhone = (String) body.getOrDefault("deliveryPartnerPhone", "9870001122");
        return ResponseEntity.ok(orderService.assignDeliveryPartner(orderId, merchantId, partnerId, partnerName, partnerPhone));
    }

    @GetMapping("/delivery/{deliveryPartnerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByDeliveryPartnerId(@PathVariable Long deliveryPartnerId) {
        return ResponseEntity.ok(orderService.getOrdersByDeliveryPartnerId(deliveryPartnerId));
    }

    @PutMapping("/{orderId}/delivery-status")
    public ResponseEntity<OrderResponse> updateDeliveryStatus(@PathVariable Long orderId,
                                                             @RequestParam(required = false) Long deliveryPartnerId,
                                                             @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "DELIVERY_ACCEPTED");
        return ResponseEntity.ok(orderService.updateDeliveryStatus(orderId, deliveryPartnerId, status));
    }

    @GetMapping("/user/{userId}/delivered-products")
    public ResponseEntity<List<Long>> getDeliveredProductIdsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getDeliveredProductIdsForUser(userId));
    }
}
