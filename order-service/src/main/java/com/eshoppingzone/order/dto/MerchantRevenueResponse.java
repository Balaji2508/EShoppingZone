package com.eshoppingzone.order.dto;

import java.util.List;

public class MerchantRevenueResponse {
    private Long merchantId;
    private Double totalRevenue;
    private Double pendingRevenue;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingOrders;
    private Integer totalUnitsSold;
    private List<OrderResponse> orders;

    public MerchantRevenueResponse() {
    }

    public MerchantRevenueResponse(Long merchantId, Double totalRevenue, Double pendingRevenue, Integer totalOrders, Integer completedOrders, Integer pendingOrders, Integer totalUnitsSold, List<OrderResponse> orders) {
        this.merchantId = merchantId;
        this.totalRevenue = totalRevenue;
        this.pendingRevenue = pendingRevenue;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.pendingOrders = pendingOrders;
        this.totalUnitsSold = totalUnitsSold;
        this.orders = orders;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getPendingRevenue() {
        return pendingRevenue;
    }

    public void setPendingRevenue(Double pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(Integer completedOrders) {
        this.completedOrders = completedOrders;
    }

    public Integer getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Integer pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public Integer getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(Integer totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public List<OrderResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderResponse> orders) {
        this.orders = orders;
    }
}
