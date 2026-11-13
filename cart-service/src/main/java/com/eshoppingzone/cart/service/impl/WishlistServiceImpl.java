package com.eshoppingzone.cart.service.impl;

import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.dto.ProductDto;
import com.eshoppingzone.cart.dto.WishlistItemResponse;
import com.eshoppingzone.cart.entity.WishlistItem;
import com.eshoppingzone.cart.exception.BadRequestException;
import com.eshoppingzone.cart.exception.ResourceNotFoundException;
import com.eshoppingzone.cart.repository.WishlistRepository;
import com.eshoppingzone.cart.service.CartService;
import com.eshoppingzone.cart.service.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistRepository wishlistRepository;
    private final CartService cartService;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8082}")
    private String productServiceUrl;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, CartService cartService, RestTemplate restTemplate) {
        this.wishlistRepository = wishlistRepository;
        this.cartService = cartService;
        this.restTemplate = restTemplate;
    }

    @Override
    public WishlistItemResponse addToWishlist(Long userId, Long productId) {
        logger.info("Adding product #{} to wishlist for user #{}", productId, userId);

        // Check for duplicate
        Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            logger.info("Product #{} is already in wishlist for user #{}, returning existing item", productId, userId);
            return mapToResponse(existing.get());
        }

        // Fetch product details
        ProductDto product = fetchProduct(productId);

        WishlistItem item = new WishlistItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setBrand(product.getBrand() != null ? product.getBrand() : "Generic");
        item.setOriginalPrice(product.getPrice());
        item.setOfferPrice(product.getFinalPrice() != null ? product.getFinalPrice() : product.getPrice());
        item.setDiscountPercentage(product.getOfferPercentage() != null ? product.getOfferPercentage() : 0.0);
        item.setImageUrl(product.getImageUrl());
        item.setCategory(product.getCategory());
        item.setStockStatus(product.getStockQuantity() != null && product.getStockQuantity() > 0 ? "In Stock" : "Out of Stock");

        WishlistItem saved = wishlistRepository.save(item);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFromWishlist(Long userId, Long productId) {
        logger.info("Removing product #{} from wishlist for user #{}", productId, userId);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found for user #" + userId + " and product #" + productId));
        wishlistRepository.delete(item);
    }

    @Override
    public CartResponse moveToCart(Long userId, Long productId) {
        logger.info("Moving product #{} from wishlist to cart for user #{}", productId, userId);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found for user #" + userId + " and product #" + productId));

        // Add to cart
        AddToCartRequest cartReq = new AddToCartRequest(
                item.getProductId(),
                item.getProductName(),
                item.getOfferPrice() != null ? item.getOfferPrice() : item.getOriginalPrice(),
                1,
                item.getImageUrl()
        );
        CartResponse cartResponse = cartService.addToCart(userId, cartReq);

        // Remove from wishlist
        wishlistRepository.delete(item);
        logger.info("Successfully moved product #{} from wishlist to cart for user #{}", productId, userId);

        return cartResponse;
    }

    @Override
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
    }

    private ProductDto fetchProduct(Long productId) {
        try {
            String url = productServiceUrl + "/api/products/" + productId;
            ProductDto p = restTemplate.getForObject(url, ProductDto.class);
            if (p == null) {
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }
            return p;
        } catch (Exception e) {
            logger.error("Failed to fetch product from product-service: {}", e.getMessage());
            throw new BadRequestException("Could not fetch product information: " + e.getMessage());
        }
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getUserId(),
                item.getProductId(),
                item.getProductName(),
                item.getBrand(),
                item.getOriginalPrice(),
                item.getOfferPrice(),
                item.getDiscountPercentage(),
                item.getImageUrl(),
                item.getCategory(),
                item.getStockStatus(),
                item.getCreatedAt()
        );
    }
}
