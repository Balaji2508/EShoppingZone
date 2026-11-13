package com.eshoppingzone.cart.service.impl;

import com.eshoppingzone.cart.dto.*;
import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.CartItem;
import com.eshoppingzone.cart.exception.BadRequestException;
import com.eshoppingzone.cart.exception.ResourceNotFoundException;
import com.eshoppingzone.cart.repository.CartItemRepository;
import com.eshoppingzone.cart.repository.CartRepository;
import com.eshoppingzone.cart.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.info("Creating new cart for user ID: {}", userId);
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setTotalAmount(0.0);
                    return cartRepository.save(newCart);
                });
    }

    @Override
    public CartResponse getCartByUserId(Long userId) {
        logger.info("Fetching cart for user ID: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        logger.info("Adding product ID: {} to cart for user ID: {}", request.getProductId(), userId);
        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setPrice(request.getPrice());
            existingItem.recalculateSubtotal();
            logger.info("Updated quantity for product ID: {} in cart. New quantity: {}", request.getProductId(), existingItem.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(request.getProductName());
            newItem.setPrice(request.getPrice());
            newItem.setQuantity(request.getQuantity());
            newItem.setImageUrl(request.getImageUrl());
            newItem.recalculateSubtotal();
            cart.getItems().add(newItem);
            logger.info("Added new item product ID: {} to cart", request.getProductId());
        }

        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    @Override
    public CartResponse updateCartItemQuantity(Long userId, Long productId, UpdateCartItemRequest request) {
        logger.info("Updating quantity for product/item ID: {} in user ID: {} cart", productId, userId);
        if (request == null || request.getQuantity() == null) {
            throw new BadRequestException("Quantity cannot be empty");
        }

        Cart cart = getOrCreateCart(userId);

        if (request.getQuantity() <= 0) {
            logger.info("Quantity is <= 0. Removing item from cart for user ID: {}", userId);
            cart.getItems().removeIf(item ->
                    (item.getProductId() != null && item.getProductId().equals(productId)) ||
                    (item.getId() != null && item.getId().equals(productId))
            );
            cart.recalculateTotal();
            Cart saved = cartRepository.save(cart);
            return mapToResponse(saved);
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> (i.getProductId() != null && i.getProductId().equals(productId)) ||
                             (i.getId() != null && i.getId().equals(productId)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + productId + " not found in user's cart"));

        item.setQuantity(request.getQuantity());
        item.recalculateSubtotal();
        cart.recalculateTotal();

        Cart saved = cartRepository.save(cart);
        logger.info("Updated quantity for product ID: {} in cart. New quantity: {}, subtotal: ₹{}", productId, item.getQuantity(), item.getSubtotal());
        return mapToResponse(saved);
    }

    @Override
    public CartResponse removeCartItem(Long userId, Long productId) {
        logger.info("Removing product/item ID: {} from user ID: {} cart", productId, userId);
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item ->
                (item.getProductId() != null && item.getProductId().equals(productId)) ||
                (item.getId() != null && item.getId().equals(productId))
        );

        if (!removed) {
            throw new ResourceNotFoundException("Product ID " + productId + " not found in user's cart");
        }

        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        logger.info("Successfully removed product/item ID: {} from cart. New cart total: ₹{}", productId, saved.getTotalAmount());
        return mapToResponse(saved);
    }

    @Override
    public void clearCart(Long userId) {
        logger.info("Clearing cart for user ID: {}", userId);
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
        logger.info("Cart cleared successfully for user ID: {}", userId);
    }

    private CartResponse mapToResponse(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getTotalAmount(),
                cart.getItems().stream()
                        .map(item -> new CartItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getPrice(),
                                item.getQuantity(),
                                item.getSubtotal(),
                                item.getImageUrl()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
