package com.eshoppingzone.product.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductResponse {
    private Long id;
    private String name;
    private String brand;
    private String description;
    private Double price;
    private String category;
    private Integer stockQuantity;
    private String imageUrl;
    private Long merchantId;
    private String merchantName;
    private Double offerPercentage;
    private Boolean offerActive;
    private Double discountAmount;
    private Double finalPrice;
    private Double averageRating;
    private Integer totalReviews;
    private List<ReviewResponse> reviews = new ArrayList<>();

    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, String brand, String description, Double price, String category,
                           Integer stockQuantity, String imageUrl, Long merchantId, String merchantName,
                           Double offerPercentage, Boolean offerActive, Double discountAmount, Double finalPrice,
                           Double averageRating, Integer totalReviews, List<ReviewResponse> reviews) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.offerPercentage = offerPercentage;
        this.offerActive = offerActive;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.reviews = reviews != null ? reviews : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public Double getOfferPercentage() {
        return offerPercentage;
    }

    public void setOfferPercentage(Double offerPercentage) {
        this.offerPercentage = offerPercentage;
    }

    public Boolean getOfferActive() {
        return offerActive;
    }

    public void setOfferActive(Boolean offerActive) {
        this.offerActive = offerActive;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(Double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<ReviewResponse> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
