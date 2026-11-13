package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.dto.WishlistItemResponse;

import java.util.List;

public interface WishlistService {
    WishlistItemResponse addToWishlist(Long userId, Long productId);
    List<WishlistItemResponse> getWishlistByUserId(Long userId);
    void removeFromWishlist(Long userId, Long productId);
    CartResponse moveToCart(Long userId, Long productId);
    void clearWishlist(Long userId);
}
