package com.eshoppingzone.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class OfferRequest {

    @NotNull(message = "Offer percentage is required")
    @DecimalMin(value = "0.0", message = "Offer percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Offer percentage cannot exceed 100%")
    private Double offerPercentage;

    private Boolean offerActive;

    private LocalDateTime offerStartDate;

    private LocalDateTime offerEndDate;

    public OfferRequest() {
    }

    public OfferRequest(Double offerPercentage, Boolean offerActive, LocalDateTime offerStartDate, LocalDateTime offerEndDate) {
        this.offerPercentage = offerPercentage;
        this.offerActive = offerActive;
        this.offerStartDate = offerStartDate;
        this.offerEndDate = offerEndDate;
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

    public LocalDateTime getOfferStartDate() {
        return offerStartDate;
    }

    public void setOfferStartDate(LocalDateTime offerStartDate) {
        this.offerStartDate = offerStartDate;
    }

    public LocalDateTime getOfferEndDate() {
        return offerEndDate;
    }

    public void setOfferEndDate(LocalDateTime offerEndDate) {
        this.offerEndDate = offerEndDate;
    }
}
