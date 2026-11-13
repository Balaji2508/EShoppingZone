package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.CartDto;
import com.eshoppingzone.order.exception.BadRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CartClient {

    private static final Logger logger = LoggerFactory.getLogger(CartClient.class);

    private final RestTemplate restTemplate;
    private final String cartServiceUrl;

    public CartClient(RestTemplate restTemplate,
                      @Value("${services.cart.url:http://localhost:8083}") String cartServiceUrl) {
        this.restTemplate = restTemplate;
        this.cartServiceUrl = cartServiceUrl;
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    public CartDto getCart(Long userId) {
        String url = cartServiceUrl + "/api/cart/" + userId;
        logger.info("Calling Cart Service: {}", url);
        try {
            return restTemplate.getForObject(url, CartDto.class);
        } catch (Exception e) {
            logger.error("Error fetching cart from Cart Service: {}", e.getMessage());
            throw new BadRequestException("Failed to retrieve user cart: " + e.getMessage());
        }
    }

    public CartDto getCartFallback(Long userId, Throwable t) {
        logger.error("[CIRCUIT BREAKER] Cart Service is temporarily unavailable for user ID {}: {}", userId, t.getMessage());
        throw new BadRequestException("Cart service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "clearCartFallback")
    public void clearCart(Long userId) {
        String url = cartServiceUrl + "/api/cart/" + userId + "/clear";
        logger.info("Calling Cart Service to clear cart: {}", url);
        try {
            restTemplate.delete(url);
            logger.info("Cart cleared successfully for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage());
        }
    }

    public void clearCartFallback(Long userId, Throwable t) {
        logger.warn("[CIRCUIT BREAKER] Cart Service unavailable for clearCart (User ID {}): {}", userId, t.getMessage());
    }
}
