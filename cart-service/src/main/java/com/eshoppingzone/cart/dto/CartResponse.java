package com.eshoppingzone.cart.dto;

import java.util.List;

public class CartResponse {
    private Long id;
    private Long userId;
    private Double totalAmount;
    private List<CartItemResponse> items;

    public CartResponse() {
    }

    public CartResponse(Long id, Long userId, Double totalAmount, List<CartItemResponse> items) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.items = items;
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

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }
}
