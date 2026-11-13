package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.CreateOrderRequest;
import com.eshoppingzone.order.exception.BadRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8087}")
    private String paymentServiceUrl;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public Map<String, Object> processPayment(CreateOrderRequest request, Double amount, Long orderId) {
        String url = paymentServiceUrl + "/api/payments/process";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("userId", request.getUserId());
        body.put("amount", amount);
        body.put("paymentMethod", request.getPaymentMethod());
        body.put("upiId", request.getUpiId());
        body.put("cardNumber", request.getCardNumber());
        body.put("cardExpiry", request.getCardExpiry());
        body.put("cardCvv", request.getCardCvv());
        body.put("cardHolderName", request.getCardHolderName());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            logger.info("Calling Payment Service at {} for user #{}, method {}", url, request.getUserId(), request.getPaymentMethod());
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            return response != null ? response : Map.of("paymentStatus", "PAID");
        } catch (HttpClientErrorException ex) {
            logger.error("Payment Service rejected payment: {}", ex.getResponseBodyAsString());
            throw new BadRequestException("Payment failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            logger.error("Error connecting to Payment Service: {}", ex.getMessage());
            if ("COD".equalsIgnoreCase(request.getPaymentMethod()) || "CASH_ON_DELIVERY".equalsIgnoreCase(request.getPaymentMethod())) {
                return Map.of("paymentStatus", "COD_PENDING");
            }
            throw new BadRequestException("Payment processing failed: " + ex.getMessage());
        }
    }

    public Map<String, Object> processPaymentFallback(CreateOrderRequest request, Double amount, Long orderId, Throwable t) {
        if (t instanceof BadRequestException) {
            throw (BadRequestException) t;
        }
        logger.error("[CIRCUIT BREAKER] Payment Service is temporarily unavailable for order #{}: {}", orderId, t.getMessage());
        if ("COD".equalsIgnoreCase(request.getPaymentMethod()) || "CASH_ON_DELIVERY".equalsIgnoreCase(request.getPaymentMethod())) {
            return Map.of("paymentStatus", "COD_PENDING");
        }
        throw new BadRequestException("Payment service is temporarily unavailable. Please try again later.");
    }
}
