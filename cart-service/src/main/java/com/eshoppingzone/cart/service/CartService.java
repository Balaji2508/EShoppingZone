package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addToCart(Long userId, AddToCartRequest request);
    CartResponse updateCartItemQuantity(Long userId, Long productId, UpdateCartItemRequest request);
    CartResponse removeCartItem(Long userId, Long productId);
    void clearCart(Long userId);
}
