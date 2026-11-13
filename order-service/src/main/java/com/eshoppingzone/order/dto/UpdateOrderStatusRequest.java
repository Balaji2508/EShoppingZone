package com.eshoppingzone.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateOrderStatusRequest {

    @NotBlank(message = "Order status is required")
    @Pattern(regexp = "^(PLACED|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)$", message = "Invalid order status")
    private String orderStatus;

    public UpdateOrderStatusRequest() {
    }

    public UpdateOrderStatusRequest(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
