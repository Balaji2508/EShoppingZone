package com.eshoppingzone.order.client;

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
public class WalletClient {

    private static final Logger logger = LoggerFactory.getLogger(WalletClient.class);

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletClient(RestTemplate restTemplate,
                        @Value("${services.wallet.url:http://localhost:8085}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    @CircuitBreaker(name = "walletService", fallbackMethod = "processPaymentFallback")
    public void processPayment(Long userId, Double amount, String description) {
        String url = walletServiceUrl + "/api/wallet/" + userId + "/pay";
        logger.info("Calling Wallet Service for payment: URL={}, UserID={}, Amount=₹{}", url, userId, amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);
        requestBody.put("description", description);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            logger.info("Wallet payment successful for user ID: {}, amount: ₹{}", userId, amount);
        } catch (HttpClientErrorException e) {
            logger.error("Wallet payment rejected: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Wallet payment failed: Insufficient wallet balance or invalid request");
        } catch (Exception e) {
            logger.error("Failed to connect to Wallet Service: {}", e.getMessage());
            throw new BadRequestException("Wallet Service unavailable or payment failed: " + e.getMessage());
        }
    }

    public void processPaymentFallback(Long userId, Double amount, String description, Throwable t) {
        if (t instanceof BadRequestException) {
            throw (BadRequestException) t;
        }
        logger.error("[CIRCUIT BREAKER] Wallet Service is temporarily unavailable for payment (User ID {}): {}", userId, t.getMessage());
        throw new BadRequestException("Wallet service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "walletService", fallbackMethod = "topupFallback")
    public void topup(Long userId, Double amount, String description) {
        String url = walletServiceUrl + "/api/wallet/" + userId + "/topup";
        logger.info("Calling Wallet Service for refund/topup: URL={}, UserID={}, Amount=₹{}", url, userId, amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            logger.info("Wallet refund/topup successful for user ID: {}, amount: ₹{}", userId, amount);
        } catch (Exception e) {
            logger.error("Failed to refund/topup wallet for user {}: {}", userId, e.getMessage());
        }
    }

    public void topupFallback(Long userId, Double amount, String description, Throwable t) {
        logger.warn("[CIRCUIT BREAKER] Wallet Service unavailable for refund topup (User ID {}): {}", userId, t.getMessage());
    }
}
