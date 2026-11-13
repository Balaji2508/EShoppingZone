package com.eshoppingzone.cart.dto;

import java.time.LocalDateTime;

public class WishlistItemResponse {

    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String brand;
    private Double originalPrice;
    private Double offerPrice;
    private Double discountPercentage;
    private String imageUrl;
    private String category;
    private String stockStatus;
    private LocalDateTime createdAt;

    public WishlistItemResponse() {
    }

    public WishlistItemResponse(Long id, Long userId, Long productId, String productName, String brand, Double originalPrice, Double offerPrice, Double discountPercentage, String imageUrl, String category, String stockStatus, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.originalPrice = originalPrice;
        this.offerPrice = offerPrice;
        this.discountPercentage = discountPercentage;
        this.imageUrl = imageUrl;
        this.category = category;
        this.stockStatus = stockStatus;
        this.createdAt = createdAt;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Double getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(Double offerPrice) {
        this.offerPrice = offerPrice;
    }

    public Double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
