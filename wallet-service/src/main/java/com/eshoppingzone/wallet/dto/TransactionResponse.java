package com.eshoppingzone.wallet.dto;

import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private Long userId;
    private Double amount;
    private String type;
    private LocalDateTime transactionDate;
    private String description;
    private String status;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, Long userId, Double amount, String type, LocalDateTime transactionDate, String description, String status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
        this.description = description;
        this.status = status;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
