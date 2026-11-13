package com.eshoppingzone.cart.controller;

import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.dto.WishlistItemResponse;
import com.eshoppingzone.cart.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/{userId}/add/{productId}")
    public ResponseEntity<WishlistItemResponse> addToWishlist(@PathVariable Long userId, @PathVariable Long productId) {
        WishlistItemResponse response = wishlistService.addToWishlist(userId, productId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(@PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlistByUserId(userId));
    }

    @DeleteMapping("/{userId}/remove/{productId}")
    public ResponseEntity<Map<String, String>> removeFromWishlist(@PathVariable Long userId, @PathVariable Long productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("message", "Product removed from wishlist", "productId", String.valueOf(productId)));
    }

    @PostMapping("/{userId}/move-to-cart/{productId}")
    public ResponseEntity<CartResponse> moveToCart(@PathVariable Long userId, @PathVariable Long productId) {
        CartResponse response = wishlistService.moveToCart(userId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearWishlist(@PathVariable Long userId) {
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(Map.of("message", "Wishlist cleared successfully"));
    }
}
