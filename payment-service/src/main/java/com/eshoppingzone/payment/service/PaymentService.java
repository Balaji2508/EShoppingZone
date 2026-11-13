package com.eshoppingzone.payment.service;

import com.eshoppingzone.payment.dto.PaymentRequest;
import com.eshoppingzone.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPaymentById(String paymentId);
    PaymentResponse getPaymentByOrderId(Long orderId);
    List<PaymentResponse> getPaymentsByUserId(Long userId);
    List<PaymentResponse> getAllPayments();
}
