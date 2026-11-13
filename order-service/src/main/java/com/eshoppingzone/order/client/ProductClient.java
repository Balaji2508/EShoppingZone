package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.ProductDto;
import com.eshoppingzone.order.exception.BadRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductClient(RestTemplate restTemplate,
                         @Value("${services.product.url:http://localhost:8082}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(Long productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        try {
            return restTemplate.getForObject(url, ProductDto.class);
        } catch (Exception e) {
            logger.warn("Could not fetch product details for ID {}: {}", productId, e.getMessage());
            return null;
        }
    }

    public ProductDto getProductFallback(Long productId, Throwable t) {
        logger.warn("[CIRCUIT BREAKER] Product Service is unavailable for getProduct(ID={}): {}", productId, t.getMessage());
        return null;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    public void reduceStock(Long productId, int quantity) {
        String url = productServiceUrl + "/api/products/" + productId + "/reduce-stock?quantity=" + quantity;
        logger.info("Calling Product Service to reduce stock: {}", url);
        try {
            restTemplate.put(url, null);
            logger.info("Stock reduced successfully for product ID: {}", productId);
        } catch (HttpClientErrorException e) {
            logger.error("Stock reduction error from Product Service: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Stock reduction failed for product ID " + productId + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Failed to connect to Product Service: {}", e.getMessage());
            throw new BadRequestException("Failed to reduce product stock: " + e.getMessage());
        }
    }

    public void reduceStockFallback(Long productId, int quantity, Throwable t) {
        if (t instanceof BadRequestException) {
            throw (BadRequestException) t;
        }
        logger.error("[CIRCUIT BREAKER] Product Service is temporarily unavailable for reduceStock(ID={}): {}", productId, t.getMessage());
        throw new BadRequestException("Product service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "restoreStockFallback")
    public void restoreStock(Long productId, int quantity) {
        String url = productServiceUrl + "/api/products/" + productId + "/restore-stock?quantity=" + quantity;
        logger.info("Calling Product Service to restore stock: {}", url);
        try {
            restTemplate.put(url, null);
            logger.info("Stock restored successfully for product ID: {}", productId);
        } catch (Exception e) {
            logger.warn("Failed to restore product stock for ID {}: {}", productId, e.getMessage());
        }
    }

    public void restoreStockFallback(Long productId, int quantity, Throwable t) {
        logger.warn("[CIRCUIT BREAKER] Product Service is unavailable for restoreStock(ID={}): {}", productId, t.getMessage());
    }
}
