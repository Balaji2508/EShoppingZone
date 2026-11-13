package com.eshoppingzone.wallet.dto;

public class WalletResponse {
    private Long id;
    private Long userId;
    private Double balance;
    private String message;

    public WalletResponse() {
    }

    public WalletResponse(Long id, Long userId, Double balance, String message) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.message = message;
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

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
