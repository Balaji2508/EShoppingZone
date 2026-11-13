package com.eshoppingzone.cart.controller;

import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.CartResponse;
import com.eshoppingzone.cart.dto.UpdateCartItemRequest;
import com.eshoppingzone.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping(value = {"/{userId}/add", "/{userId}/items"})
    public ResponseEntity<CartResponse> addToCart(@PathVariable Long userId,
                                                   @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping(value = {"/{userId}/item/{productId}", "/{userId}/items/{productId}"})
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable Long userId,
                                                        @PathVariable Long productId,
                                                        @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, productId, request));
    }

    @DeleteMapping(value = {"/{userId}/item/{productId}", "/{userId}/items/{productId}"})
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable Long userId,
                                                        @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeCartItem(userId, productId));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }
}
